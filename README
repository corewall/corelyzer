Corelyzer

Corelyzer is a scalable, extensible visualization tool developed to enhance the
study of geological cores. The strength of Corelyzer is the ability to display
large sets of core imagery, with multi-sensor logs, annotations and plugin
supported visuals alongside the core imagery. This document describes how to
download, setup and use the software.

# Building

Corelyzer consists of two pieces: the SceneGraph library and the Corelyzer 
application itself.  The SceneGraph library uses native code and must be 
compiled for each platform Corelyzer runs on.  The Corelyzer application itself
is written in Java and can be built on any platform.  To build the Corelyzer
application use the provided Gradle tool:

  sh gradlew clean package

This will compile the application using a pre-built versions of the SceneGraph
library.  It will package Corelyzer up for Mac and Windows in the dist/ 
directory.

Building the SceneGraph library is slightly more complicated as it needs to be
built for each platform and architecture Corelyzer runs on.  It only needs to
be re-built if you change the native code or if you increment the version 
number.  Full build instructions are provided in: scenegraph/BUILD

Copyright (c) 2010, Core Wall Project (http://corewall.org)
