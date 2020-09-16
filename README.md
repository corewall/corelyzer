# Corelyzer

Corelyzer is a scalable, extensible visualization tool developed to enhance the
study of geological cores. The strength of Corelyzer is the ability to display
large sets of core imagery, with multi-sensor logs, annotations and plugin
supported visuals alongside the core imagery. This document describes how to
download, setup and use the software.

Corelyzer consists of two pieces: the SceneGraph library and the Corelyzer 
application itself.  The SceneGraph library uses native code and must be 
compiled for each platform Corelyzer runs on.  The Corelyzer application
is written in Java and can be built on any platform.

### Building Corelyzer on Linux

At present, Corelyzer can be built and run on Ubuntu 18.04.4 "Bionic Beaver"
with Linux kernel version 5.3.0-28-generic.

Note that Corelyzer **will not run** against kernel versions 5.4+, it crashes at launch.

On Linux, no pre-built SceneGraph library is provided.

To build, follow the instructions in scenegraph/README.md.

Once SceneGraph is built, build Corelyzer with:
    
    [root corelyzer dir]% ./gradlew clean packageLinux

This will build Java components, create corelyzer/working_dir, and
copy SceneGraph and other resources to working_dir.

To run Corelyzer:

    cd working_dir
    java -cp "../app/dist/*" -Djava.library.path=/home/lcdev/proj/corewall/corelyzer/scenegraph/dist:/usr/lib/x86_64-linux-gnu/ corelyzer.ui.splashscreen.SplashScreenMain

Adjust java.library.path to reflect your configuration if needed.

Corelyzer should launch. Have fun!


### Building Corelyzer on Windows and Mac

Use the provided Gradle wrapper:

  ./gradlew clean package[Mac or Win]

This will compile the Java application using a pre-built versions of the
SceneGraph library.  It will package Corelyzer up for Mac and Windows in the
dist/ directory.

Building the SceneGraph library is slightly more complicated as it needs to be
built for each platform and architecture Corelyzer runs on.  It only needs to
be re-built if you change the native code or if you increment the version 
number.  Build instructions are provided in scenegraph/README.md

Copyright (c) 2020, CSDF (http://csdco.umn.edu)
