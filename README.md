### Corelyzer

Corelyzer is a scalable, extensible visualization tool developed to enhance the
study of geological cores. The strength of Corelyzer is the ability to display
large sets of core imagery, with multi-sensor logs, annotations and plugin
supported visuals alongside the core imagery.

Corelyzer builds can be downloaded on the [releases](https://github.com/corewall/corelyzer/releases) page.


#### Building Corelzer
Corelyzer consists of two pieces:
- the Scenegraph library, written in C++
- the Corelyzer application itself, written in Java

Pre-built Scenegraph native binaries are provided in
deps/win, deps/mac, and deps/linux.

To build Corelyzer using a pre-built Scenegraph library:

    gradlew clean package[platform]
    
where [platform] is 'mac', 'windows', or 'linux'.

For Windows and Mac, the generated Corelyzer application can be found in the
dist/[platform] directory.

For Linux, there is no packaged build. The packageLinux task compiles Corelyzer Java
logic into dist/app-[version].jar and copies all required JARs to dist. It then
creates a corelyzer/working_dir directory and copies required resources there.

From working_dir, launch Corelyzer with command (adjusting paths for your system):

    java -cp "../app/dist/*" -Djava.library.path=/home/lcdev/proj/corewall/corelyzer/scenegraph/dist:/usr/lib/x86_64-linux-gnu corelyzer.ui.splashscreen.SplashScreenMain

##### Scenegraph
See scenegraph/README for instructions on building the Scenegraph library
for your platform.


Copyright (c) 2020, CSDCO (https://csdco.umn.edu/resources/software/corelyzer)
