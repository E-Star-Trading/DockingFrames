"""Upload a built common jar to the e-star Azure Artifacts Maven feed, without a Maven installation.

Why this exists
---------------
The published artifact is not what this repository's reactor builds. The reactor is
org.dockingframes:docking-frames-common:1.1.3p1; the feed carries
docking:e-star-docking-frames-common:1.1.3pN, with a bare pom that names only the three coordinates.
There is no <distributionManagement> anywhere in the poms, so `mvn deploy` has no target at all and
`mvn deploy:deploy-file` needs a Maven installation plus a settings.xml server entry that this
machine does not have. An Azure Artifacts Maven feed is an ordinary Maven repository behind basic
auth, so the six files a deploy consists of can simply be PUT.

What it uploads, and in which order
-----------------------------------
    <artifact>-<version>.pom  + .pom.sha1 + .pom.md5
    <artifact>-<version>.jar  + .jar.sha1 + .jar.md5
and then merges the version into the artifact's maven-metadata.xml, which on this feed is a stored
file rather than something the server derives - it still says lastUpdated 2023-01-13. Nothing in EETS
needs it, because every module pins an exact version, so --skip-metadata leaves a usable publish.

Why a dry run is the default
----------------------------
Azure Artifacts versions are immutable: a wrong or premature 1.1.3p5 cannot be replaced, only
succeeded by a p6, and every consumer that already resolved p5 keeps the bad one. So the upload
happens only with --publish, and four things are checked before the first byte goes out:

  * the version must not already exist in the feed - the overwrite would be refused anyway, and the
    early failure says so in one line instead of halfway through the six files;
  * every class in the jar must be Java 8 or older, the same guard rebuild_local_jar.py applies,
    because a jar published at class file major 69 is a release that cannot be taken back;
  * the jar must differ from the currently published version, and the differing classes are printed:
    publishing an unchanged jar under a new number is the mistake that is easiest to make and
    hardest to notice;
  * the PAT must be present and carry Packaging read & write.

Usage
-----
    python tools/publish_to_feed.py                  # dry run: checks everything, uploads nothing
    python tools/publish_to_feed.py --publish
    python tools/publish_to_feed.py --version 1.1.3p6 --jar <path> --publish
"""
import argparse
import base64
import hashlib
import io
import os
import re
import struct
import sys
import urllib.error
import urllib.request
import zipfile
from datetime import datetime, timezone

JAVA_8_MAJOR = 52
FEED = 'https://pkgs.dev.azure.com/e-star-trading/_packaging/Externals/maven/v1'
GROUP = 'docking'
ARTIFACT = 'e-star-docking-frames-common'

POM = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>%s</groupId>
  <artifactId>%s</artifactId>
  <version>%s</version>
</project>
"""


def parse_args():
    default_jar = os.path.expanduser(
        '~/.m2/repository/docking/e-star-docking-frames-common/1.1.3p5-local/'
        'e-star-docking-frames-common-1.1.3p5-local.jar')
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--jar', default=default_jar, help='the jar to publish')
    p.add_argument('--version', default='1.1.3p5', help='the version to publish it as')
    p.add_argument('--against', default=None,
                   help='the published version to diff against (default: the feed\'s current release)')
    p.add_argument('--feed', default=FEED)
    p.add_argument('--publish', action='store_true', help='actually upload; without it nothing is written')
    p.add_argument('--skip-metadata', action='store_true', help='leave maven-metadata.xml alone')
    p.add_argument('--allow-identical', action='store_true',
                   help='publish even when the jar matches the version it was diffed against')
    return p.parse_args()


def auth_header():
    token = os.environ.get('AZURE_PERSONAL_ACCESS_TOKEN')
    if not token:
        sys.exit('AZURE_PERSONAL_ACCESS_TOKEN is not set - it needs Packaging read & write')
    return 'Basic ' + base64.b64encode((':' + token).encode()).decode()


def get(url, auth):
    """The response body, or None for a 404. Any other failure is fatal."""
    request = urllib.request.Request(url, headers={'Authorization': auth})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return response.read()
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        if e.code in (401, 403):
            sys.exit('%d from the feed - the PAT is missing Packaging read & write, or has expired' % e.code)
        sys.exit('GET %s failed: %s' % (url, e))


def put(url, body, auth, dry_run):
    if dry_run:
        print('    would PUT %7d bytes  %s' % (len(body), url.rsplit('/', 1)[1]))
        return
    request = urllib.request.Request(url, data=body, method='PUT',
                                     headers={'Authorization': auth,
                                              'Content-Type': 'application/octet-stream',
                                              'Content-Length': str(len(body))})
    try:
        with urllib.request.urlopen(request, timeout=300) as response:
            print('    %d %7d bytes  %s' % (response.status, len(body), url.rsplit('/', 1)[1]))
    except urllib.error.HTTPError as e:
        if e.code in (401, 403):
            sys.exit('%d on upload - the PAT needs Packaging read & write on the Externals feed' % e.code)
        if e.code == 409:
            sys.exit('409 conflict - this version already exists and Azure Artifacts never replaces one')
        sys.exit('PUT %s failed: %s\n%s' % (url, e, e.read().decode('utf-8', 'replace')[:500]))


def classes_of(jar_bytes, where):
    """Every class in the jar by name, asserted to be Java 8 or older before anything is uploaded."""
    out = {}
    with zipfile.ZipFile(jar_bytes) as z:
        for name in z.namelist():
            if not name.endswith('.class'):
                continue
            data = z.read(name)
            major = struct.unpack('>H', data[6:8])[0]
            if major > JAVA_8_MAJOR:
                sys.exit('%s in %s is class file major %d - that jar must not be published'
                         % (name, where, major))
            out[name] = hashlib.sha1(data).hexdigest()
    return out


def merged_metadata(existing, version):
    """The feed's metadata with `version` added, or None when it is already listed."""
    text = existing.decode('utf-8')
    if '<version>%s</version>' % version in text:
        return None
    versions = re.search(r'(<versions>)(.*?)(</versions>)', text, re.S)
    if not versions:
        sys.exit('maven-metadata.xml has no <versions> block - refusing to guess at its shape')
    # after the last listed version, not after the block's trailing whitespace, or the new line lands
    # past the newline and </versions> ends up sharing a line with it
    last = text.rindex('</version>', versions.start(2), versions.end(2)) + len('</version>')
    text = text[:last] + '\n      <version>%s</version>' % version + text[last:]
    text = re.sub(r'<latest>.*?</latest>', '<latest>%s</latest>' % version, text)
    text = re.sub(r'<release>.*?</release>', '<release>%s</release>' % version, text)
    stamp = datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S')
    text = re.sub(r'<lastUpdated>.*?</lastUpdated>', '<lastUpdated>%s</lastUpdated>' % stamp, text)
    # the version just inserted sits last in the block; <version> at the top level is the old release
    text = re.sub(r'(<metadata>\s*<groupId>.*?</groupId>\s*<artifactId>.*?</artifactId>\s*)<version>.*?</version>',
                  r'\g<1><version>%s</version>' % version, text, flags=re.S)
    return text.encode('utf-8')


def upload(base, name, body, auth, dry_run):
    put('%s/%s' % (base, name), body, auth, dry_run)
    put('%s/%s.sha1' % (base, name), hashlib.sha1(body).hexdigest().encode(), auth, dry_run)
    put('%s/%s.md5' % (base, name), hashlib.md5(body).hexdigest().encode(), auth, dry_run)


def main():
    args = parse_args()
    auth = auth_header()
    dry_run = not args.publish

    if not os.path.isfile(args.jar):
        sys.exit('no such jar: ' + args.jar)
    jar = open(args.jar, 'rb').read()
    mine = classes_of(io.BytesIO(jar), os.path.basename(args.jar))
    print('jar        : %s' % args.jar)
    print('             %d bytes, %d classes, none above Java 8' % (len(jar), len(mine)))

    artifact_base = '%s/%s/%s' % (args.feed, GROUP.replace('.', '/'), ARTIFACT)
    version_base = '%s/%s' % (artifact_base, args.version)

    if get('%s/%s-%s.pom' % (version_base, ARTIFACT, args.version), auth) is not None:
        sys.exit('%s %s is already in the feed - Azure Artifacts never replaces a version, publish a '
                 'higher one' % (ARTIFACT, args.version))
    print('target     : %s %s (not in the feed yet)' % (ARTIFACT, args.version))

    metadata = get('%s/maven-metadata.xml' % artifact_base, auth)
    against = args.against
    if not against and metadata:
        release = re.search(r'<release>(.*?)</release>', metadata.decode('utf-8'))
        against = release.group(1) if release else None

    if against:
        published = get('%s/%s/%s-%s.jar' % (artifact_base, against, ARTIFACT, against), auth)
        if published is None:
            sys.exit('cannot read the published %s to compare against' % against)
        theirs = classes_of(io.BytesIO(published), against)
        changed = sorted(n for n in set(mine) | set(theirs) if mine.get(n) != theirs.get(n))
        print('against    : %s' % against)
        if not changed:
            print('             identical - every class matches')
            if not args.allow_identical:
                sys.exit('refusing to publish a jar that is byte-for-byte the published %s; pass '
                         '--allow-identical if that really is the intent' % against)
        else:
            print('             %d class(es) differ:' % len(changed))
            for name in changed:
                state = 'added' if name not in theirs else ('removed' if name not in mine else 'changed')
                print('               %-8s %s' % (state, name))

    print()
    print('publishing' if args.publish else 'DRY RUN - nothing is written, pass --publish to upload')
    pom = (POM % (GROUP, ARTIFACT, args.version)).encode('utf-8')
    upload(version_base, '%s-%s.pom' % (ARTIFACT, args.version), pom, auth, dry_run)
    upload(version_base, '%s-%s.jar' % (ARTIFACT, args.version), jar, auth, dry_run)

    if args.skip_metadata:
        print('  maven-metadata.xml left alone; exact-version resolution does not read it')
    elif metadata is None:
        print('  no maven-metadata.xml on the feed - leaving it that way')
    else:
        updated = merged_metadata(metadata, args.version)
        if updated is None:
            print('  maven-metadata.xml already lists %s' % args.version)
        else:
            upload(artifact_base, 'maven-metadata.xml', updated, auth, dry_run)

    if dry_run:
        return 0

    print()
    fetched = get('%s/%s-%s.jar' % (version_base, ARTIFACT, args.version), auth)
    if fetched is None or hashlib.sha1(fetched).hexdigest() != hashlib.sha1(jar).hexdigest():
        sys.exit('the feed does not serve back what was uploaded - check the feed before building against it')
    print('verified   : the feed serves back the same %d bytes' % len(fetched))
    print('next       : set MarketMaker/build.gradle to %s and delete '
          '~/.gradle/init.d/estar-docking-local.gradle' % args.version)
    return 0


if __name__ == '__main__':
    sys.exit(main())
