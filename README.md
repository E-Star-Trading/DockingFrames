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
