### Building Scenegraph

The Corelyzer Scenegraph Library requires some initial setup to build completely from source.  We provide pre-built versions of the library, so you only need to follow these instructions if you intend to modify Scenegraph.

#### macOS/Mac OSX

Xcode and command-line developer tools must be installed.

Includes and libraries for the required libtiff, libjpeg, libpng, freetype, and squish are provided in `scenegraph/deps/mac`.

Modify JDK and system include paths in `scenegraph/build.gradle` to reflect your configuration.

Then, in the `scenegraph` dir, build Corelyzer with the included Gradle wrapper:

    ../gradlew clean build-jni-mac

The generated Scenegraph JAR file and native macOS binary (jnilib) will be found in `scenegraph/dist`.


#### Windows

The Windows Scenegraph DLL can be cross-built on non-Windows systems with MXE, or built natively on Windows with Visual Studio 2008.

##### Cross-build on macOS and Linux

This method is preferred as it 1) produces a statically-linked DLL and 2) doesn't require an ancient paid product (Visual Studio 2008).

###### Prepare MXE Cross Environment

A scenegraph DLL can be built on macOS or Linux with the [MXE Cross Environment](https://mxe.cc). The following instructions are Mac-specific, but should translate to Linux easily.


1. Follow [Step 1 of the Tutorial](https://mxe.cc/#tutorial) to download MXE.

2. Create a `settings.mk` file with the following lines, and save it to the root `mxe` dir.

```
MXE_TARGETS := x86_64-w64-mingw32.static
MXE_USE_CCACHE := no
```

3. Follow [Step 2 of the Tutorial](https://mxe.cc/#tutorial) to install MXE system-wide.

Scenegraph build scripts assume MXE is installed in the suggested `/opt/mxe`.

4. In the `mxe` root dir, `make cc` to build the cross-compile toolchain. The process will take a while.

5. Once `make cc` completes, follow [Step 4 of the Tutorial](https://mxe.cc/#tutorial), adding the newly-built cross-compile binaries to the beginning of your `PATH`.

6. Now Scenegraph dependencies can be cross-built for Windows.

###### Cross-build Scenegraph Dependencies

Make sure `/opt/mxe/usr/bin` is at the beginning of your shell's `PATH` before proceeding.

Scenegraph depends on the following libraries: `libpng jpeg tiff pthreads freetype libsquish`

The first four are included with MXE, and can be built with no further effort. In the `mxe` root dir:
`make libpng jpeg tiff pthreads`

`freetype` is also included with MXE, but requires [Perl-Compatible Regular Expressions](https://www.pcre.org/) to be installed natively on Mac to cross-build. To install PCRE: `brew install pcre`

Now `make freetype` should build successfully.

Finally, build the `libsquish` dependency, which is not included in MXE.
1. Download `libsquish` source [here](https://sourceforge.net/projects/libsquish/files/).
2. Download and install [CMake](https://cmake.org/download/), which is used to prepare Makefiles for cross-building.
3. Run CMake.app. For "Where is the source code", choose the `libsquish` root dir. Choose any dir for "Where to build the binaries".
4. Click the Configure button. Select the "Specify toolchain file for cross-compiling" option and click Done.
5. When prompted to specify the toolchain file, enter `/opt/mxe/usr/x86_64-w64-mingw32.static/share/cmake/mxe-conf.cmake`, then click Done.
6. Click the Generate button.
7. Cross-build with the prepared Makefile. In the `libsquish` root dir: `make`
8. You should now see `libsquish.a` in the `libsquish` root dir.

###### Cross-build Scenegraph DLL

In `scenegraph/build.gradle`, examine the paths in the `crossBuildJNIWin`, `crossCompileWin`, and `linkCrossCompiledWin` tasks and adjust them to reflect your MXE and Java Development Kit install paths.

Then, in the `scenegraph` dir, cross-build the DLL with `../gradlew --info crossBuildJNIWin`

The generated `scenegraph.dll` will be placed in the `scenegraph/dist` directory.


##### Native Build with Visual Studio 2008

Required libraries and headers are included in `scenegraph/deps/win64`. Open `win32/vs2008/libscenegraph.vcproj` in Visual Studio 2008 and Rebuild Solution. If all goes well, scenegraph.dll will be found in the Debug or Release subdirectory.


#### Linux

The following steps result in a working build of Scenegraph on
a on Ubuntu LTS 18.04.4 "Bionic Beaver" and 20.04.1 "Focal Fossa".

**NOTE**: Corelyzer is known to crash when launched on Ubuntu virtual machines with kernel version 5.4+. This can be resolved by disabling hardware acceleration in the VM.

Install required dev tools:

    sudo apt install git
    sudo apt install openjdk-11-jdk
    sudo apt install mesa-common-dev libglu1-mesa-dev
    sudo apt install libfreetype6-dev libpng-dev libjpeg-dev libtiff-dev libsquish-dev

Clone the Corelyzer GitHub repo and move to the `scenegraph` dir:

    git clone https://github.com/corewall/corelyzer
    cd scenegraph

In `scenegraph/build.gradle`, modify the Java include paths in the `build-jni-linux` task
to reflect your configuration.

Use the provided Gradle wrapper to build:

    ../gradlew build-jni-linux

This will generate `libscenegraph.so` and `scenegraph-[version].jar` in `scenegraph/dist`.

Follow the instructions in `corelyzer/README.md` to build the Java components of Corelyzer and launch the application.

