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
a clean Ubuntu 18.04.4 Bionic Beaver installation, with Linux
kernel version 5.3.0-28-generic.

**Do not** upgrade the kernel to 5.4+, as it causes a crash at launch.
This also means Corelyzer can't currently run on Ubuntu 20.04.1, which
includes kernel version 5.4.0-47-generic.

Install required dev tools:

    sudo apt install git
    sudo apt install openjdk-11-jdk
    sudo apt install mesa-common-dev libglu1-mesa-dev
    sudo apt install libfreetype6-dev libpng-dev libjpeg-dev libtiff-dev libsquish-dev

Clone the Corelyzer GitHub repo and checkout the linux_build branch:

    git clone https://github.com/corewall/corelyzer
    cd corelyzer
    git checkout linux_build

    cd scenegraph

Update the Java include paths in the build-jni-linux task in build.gradle
to reflect your configuration.

Use the provided Gradle wrapper to build:

    ../gradlew build-jni-linux

This will generate libscenegraph.so and scenegraph-[version].jar in scenegraph/dist.

Follow the instructions in corelyzer/README.md to build the Java components of
Corelyzer and launch the application.

