# Readme
## _Original Readme_
https://github.com/Benoker/DockingFrames/blob/master/README.md

## _E-Star-Trading_
[![N|Solid](https://cdn.join.com/62f385ab4b47aa0008214f50/e-star-trading-gmb-h-logo-m.png)](https://e-star.com/)

E-Star-Trading had adjusted the eclipse theme for its own use. Please set up your code as follows to make everything work properly.

## pom.xml
You'll need Java 8 for this project. Either create JAVA_HOME_8 in your environment variables or update the pom.xml.

## theme
Define following UI Manager keys in your code:
- Dock.hoverBackground
- icon.color
- Dock.foreground
- Dock.selectedBackground
- Dock.background
- Panel.group.background
- Dock.title.border - provide your own implementation for round corner border (simple example: https://stackoverflow.com/questions/15025092/border-with-rounded-corners-transparency)

## local test jar
Changing a theme class and trying it in a client without publishing a release: `tools/rebuild_local_jar.py`
recompiles the named classes, splices them into the published `1.1.3p4` artifact and installs the result to
`~/.m2` as `1.1.3p5-local`, which the consuming build can substitute in.

```
python tools/rebuild_local_jar.py --class XEclipseTabPainter
```

Two rules in there are not optional, and both exist because of a failure that only showed up at client
startup:

- **Compile with `--release 8`, and name the compiler.** The first hand-rolled version used whatever `javac`
  was first on the `PATH` - a JDK 25 - so the spliced classes came out at class file major 69 while the rest
  of the jar was major 52. The client runs on Java 17 and threw `UnsupportedClassVersionError`. Matching
  `pom.xml`'s `<javaVersion>1.8</javaVersion>` also keeps the local jar equal to what a real build produces.
- **Assert the class file version before packing, and rescan afterwards.** Nothing between the compiler and
  a running client looks at a class file version, so the assertion is the only check there is.

The script also replaces a top-level class together with its nested and anonymous classes. Replacing only the
names `javac` emits leaves orphans behind - `1.1.3p4` carries an `XEclipseTabPainter$2` that the current
source no longer produces.

The jar is locked while a client or a build daemon holds it; the script retries the swap and then says so.
