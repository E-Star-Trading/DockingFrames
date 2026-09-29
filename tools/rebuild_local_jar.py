"""Splice recompiled classes into a published common jar and install the result to ~/.m2.

Why this exists
---------------
Iterating on the customised eclipse theme means getting a changed class into a client without
publishing a release. Building the whole reactor needs Java 8 and a working maven; recompiling the
handful of touched classes and swapping them into the published artifact does not.

Why --release 8, and why the assertion
--------------------------------------
The first hand-rolled version of this used whatever javac was first on the PATH. That was a JDK 25,
so the spliced classes came out at class file major 69, while the other 738 classes in the jar were
major 52. The consuming client runs on Java 17 (major 61) and died on startup with
UnsupportedClassVersionError - after the jar had already been installed, because nothing in between
looked at a class file version.

So this script does three things that the ad-hoc version did not:

  * compiles with an explicitly named javac and --release 8, matching pom.xml's <javaVersion>1.8</>,
    so the local jar is what a real build of this repository would produce rather than merely
    something the current runtime tolerates;
  * asserts every emitted class is major 52 BEFORE it packs anything;
  * rescans the finished jar and reports any class above major 52.

Keep all three. The failure they prevent does not show up until a client starts.

Usage
-----
    python tools/rebuild_local_jar.py                     # defaults below
    python tools/rebuild_local_jar.py --class XEclipseTabPainter --version 1.1.3p5-local
    python tools/rebuild_local_jar.py --base-jar <published common jar> --core-jar <core jar>

The jar is written to a .tmp and swapped in, and the swap is retried: the file is locked while a
client or a build daemon holds it, and the usual fix is to close the client.
"""
import argparse
import glob
import os
import shutil
import struct
import subprocess
import sys
import time
import zipfile

JAVA_8_MAJOR = 52
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACKAGE = 'bibliothek/gui/dock/common/customized/'
SOURCE_ROOT = os.path.join(REPO, 'docking-frames-common', 'src')


def find_one(pattern, what):
    hits = [h for h in glob.glob(pattern, recursive=True) if 'sources' not in os.path.basename(h)]
    if not hits:
        sys.exit('no %s found matching %s - pass it explicitly' % (what, pattern))
    return sorted(hits)[0]


def parse_args():
    cache = os.path.expanduser('~/.gradle/caches')
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--class', dest='classes', action='append', default=None,
                   help='simple name of a class in %s to recompile and replace (repeatable)' % PACKAGE)
    p.add_argument('--version', default='1.1.3p5-local', help='version to install as in ~/.m2')
    p.add_argument('--base-jar', default=None, help='published common jar to splice into')
    p.add_argument('--core-jar', default=None, help='docking-frames-core jar, for the compile classpath')
    p.add_argument('--javac', default=None, help='the javac to compile with; must support --release 8')
    p.add_argument('--out', default=None, help='scratch directory for the compiled classes')
    p.add_argument('--retries', type=int, default=20, help='swap attempts while the jar is locked')
    a = p.parse_args()
    a.classes = a.classes or ['XEclipseTabPainter']
    a.base_jar = a.base_jar or find_one(cache + '/**/e-star-docking-frames-common-1.1.3p4.jar', 'base jar')
    a.core_jar = a.core_jar or find_one(cache + '/**/e-star-docking-frames-core-*.jar', 'core jar')
    a.javac = a.javac or default_javac()
    a.out = a.out or os.path.join(REPO, 'target', 'local-jar-classes')
    return a


def default_javac():
    # the known JDK 17 first and JAVA_HOME second, deliberately: JAVA_HOME on the machine this was
    # written for is a JDK 21, and while --release 8 makes that produce major 52 all the same, the
    # compiler that the consuming client was verified against is the one worth defaulting to
    for base in [r'C:\Program Files\Java\jdk-17', os.environ.get('JAVA_HOME')]:
        if base:
            javac = os.path.join(base, 'bin', 'javac.exe' if os.name == 'nt' else 'javac')
            if os.path.isfile(javac):
                return javac
    sys.exit('no javac found - pass --javac explicitly; do NOT rely on the PATH')


def compile_classes(args):
    if os.path.isdir(args.out):
        shutil.rmtree(args.out)
    os.makedirs(args.out)
    sources = [os.path.join(SOURCE_ROOT, PACKAGE.replace('/', os.sep) + name + '.java') for name in args.classes]
    for s in sources:
        if not os.path.isfile(s):
            sys.exit('no such source: ' + s)
    cmd = [args.javac, '--release', '8', '-nowarn',
           '-cp', args.core_jar + os.pathsep + args.base_jar, '-d', args.out] + sources
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        sys.exit('COMPILE FAILED\n' + result.stdout + result.stderr)
    print('compiled %d source(s) with %s --release 8' % (len(sources), args.javac))


def collect(args):
    """Every emitted class, asserted to be Java 8 before anything is packed."""
    emitted = {}
    for name in args.classes:
        found = glob.glob(os.path.join(args.out, PACKAGE.replace('/', os.sep), name + '*.class'))
        if not found:
            sys.exit('javac emitted nothing for ' + name)
        for path in found:
            data = open(path, 'rb').read()
            major = struct.unpack('>H', data[6:8])[0]
            if major != JAVA_8_MAJOR:
                sys.exit('%s is class file major %d, expected %d - wrong javac or wrong --release'
                         % (os.path.basename(path), major, JAVA_8_MAJOR))
            emitted[PACKAGE + os.path.basename(path)] = data
    print('%d class file(s), every one major %d' % (len(emitted), JAVA_8_MAJOR))
    return emitted


def is_stale(name, classes):
    """True for a class that belongs to one of the replaced top-level classes.

    Replacing only the names javac happened to emit is not enough: the published 1.1.3p4 carries
    XEclipseTabPainter$2, an anonymous class of a source revision that no longer has one, and leaving
    it behind puts an orphan class in the jar (caught by the class count going 741 -> 742). Match the
    top-level name and its nested and anonymous classes, and nothing else - a plain prefix test would
    also swallow an unrelated XEclipseTabPainterSomething.
    """
    if not name.startswith(PACKAGE) or not name.endswith('.class'):
        return False
    simple = name[len(PACKAGE):-len('.class')]
    return any(simple == c or simple.startswith(c + '$') for c in classes)


def install(args, emitted):
    home = os.path.expanduser('~/.m2/repository/docking/e-star-docking-frames-common/' + args.version)
    os.makedirs(home, exist_ok=True)
    jar = os.path.join(home, 'e-star-docking-frames-common-' + args.version + '.jar')
    tmp = jar + '.tmp'

    kept = dropped = 0
    with zipfile.ZipFile(args.base_jar) as source, zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as target:
        for item in source.infolist():
            if is_stale(item.filename, args.classes):
                dropped += 1
                continue
            target.writestr(item, source.read(item.filename))
            kept += 1
        for name in sorted(emitted):
            target.writestr(name, emitted[name])

    for attempt in range(1, args.retries + 1):
        try:
            os.replace(tmp, jar)
            break
        except OSError as e:
            if attempt == args.retries:
                sys.exit('could not swap in %s after %d attempts (%s).\n'
                         'The jar is locked - close the client and any build daemon holding it.' % (jar, attempt, e))
            print('  locked, retrying (%d/%d)' % (attempt, args.retries))
            time.sleep(3)

    pom = os.path.join(home, 'e-star-docking-frames-common-' + args.version + '.pom')
    if not os.path.isfile(pom):
        open(pom, 'w').write(
            '<project xmlns="http://maven.apache.org/POM/4.0.0">\n'
            '  <modelVersion>4.0.0</modelVersion>\n'
            '  <groupId>docking</groupId>\n'
            '  <artifactId>e-star-docking-frames-common</artifactId>\n'
            '  <version>%s</version>\n</project>\n' % args.version)
    print('installed  : %s' % jar)
    print('kept %d entries, replaced %d with %d' % (kept, dropped, len(emitted)))
    return jar


def rescan(jar):
    with zipfile.ZipFile(jar) as z:
        bad = []
        classes = 0
        for name in z.namelist():
            if name.endswith('.class'):
                classes += 1
                major = struct.unpack('>H', z.read(name)[6:8])[0]
                if major > JAVA_8_MAJOR:
                    bad.append((name, major))
    print('rescan     : %d classes, above major %d: %s' % (classes, JAVA_8_MAJOR, bad if bad else 'none'))
    if bad:
        sys.exit('the installed jar carries class files newer than Java 8')


if __name__ == '__main__':
    arguments = parse_args()
    compile_classes(arguments)
    rescan(install(arguments, collect(arguments)))
