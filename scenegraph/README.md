### Building Scenegraph

The Corelyzer Scenegraph Library uses native code, so requires some initial 
setup to build completely from source.  We provide pre-built 
versions of the library, so you will only need to follow these instructions if
you intend to change the native code.

#### macOS/Mac OSX

Xcode and command-line developer tools must be installed.

Includes and libraries for the required libtiff, libjpeg,
libpng, freetype, and squish are provided in deps/mac.

To build Corelyzer using the provided Gradle wrapper:

    ../gradlew clean build-jni-mac

The generated Scenegraph JAR file and jnilib will be found in scenegraph/dist.


#### Windows

The win32 version of scenegraph was formerly built via cross-compile, but
intractable runtime issues precipitated building on Windows using Microsoft's
Visual Studio 2008.  Required libraries and headers are included. Open

win32/vs2008/libscenegraph.vcproj

and Rebuild Solution. If all goes well,
scenegraph.dll can be found in the Debug or Release subdirectory.


#### Linux

The following steps result in a working build of Scenegraph on
a on Ubuntu LTS 18.04.4 "Bionic Beaver" and 20.04.1 "Focal Fossa".

**NOTE**: Corelyzer is known to crash when launched on Ubuntu virtual machines with kernel version
5.4+. This can be resolved by disabling hardware acceleration in the VM.

Install required dev tools:

    sudo apt install git
    sudo apt install openjdk-11-jdk
    sudo apt install mesa-common-dev libglu1-mesa-dev
    sudo apt install libfreetype6-dev libpng-dev libjpeg-dev libtiff-dev libsquish-dev

Clone the Corelyzer GitHub repo and move to the scenegraph dir:

    git clone https://github.com/corewall/corelyzer
    cd scenegraph

In scenegraph/build.gradle, modify the Java include paths in the `build-jni-linux` task
to reflect your configuration.

Use the provided Gradle wrapper to build:

    ../gradlew build-jni-linux

This will generate libscenegraph.so and scenegraph-[version].jar in scenegraph/dist.

Follow the instructions in corelyzer/README.md to build the Java components of
Corelyzer and launch the application.

