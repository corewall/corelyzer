## Building Scenegraph

The Corelyzer Scenegraph Library requires some initial setup to build from source.  We provide pre-built versions of the library, so you only need to follow these instructions if you intend to modify Scenegraph.

### macOS/Mac OSX

Xcode and command-line developer tools must be installed.

Includes and libraries for the required libtiff, libjpeg, libpng, freetype, and squish are provided in `scenegraph/deps/mac`.

Modify JDK and system include paths in `scenegraph/build.gradle` to reflect your configuration.

Then, in the `scenegraph` dir, build Corelyzer with the included Gradle wrapper:

    ../gradlew clean buildJNIMac

The generated Scenegraph JAR file and native macOS binary (jnilib) will be found in `scenegraph/dist`.


### Windows

The Windows Scenegraph DLL can be cross-built with MXE/mingw, or built natively on Windows with Visual Studio.

#### Cross-build on macOS, Linux, or Windows

A Scenegraph DLL can be built on macOS, Linux, or Windows (through [WSL](https://learn.microsoft.com/en-us/windows/wsl/)) with the [MXE Cross Environment](https://mxe.cc).  

This method is used to create the Scenegraph DLL included in official builds of Corelyzer.
It produces a single, statically-linked scenegraph DLL that includes all dependencies.

#### Preparing the MXE Cross Environment on Mac

The following instructions are based on a M2 Mac running macOS 26 (Tahoe), which has quirks that must be worked around to successfully cross-build scenegraph for Windows.  

1. Clone the `macos-fixes` branch of the `allquixotic` fork of the MXE project:  
`git clone -b macos-fixes https://github.com/allquixotic/mxe`

2. In the root `mxe` dir, create a `settings.mk` file with the following lines:

```
MXE_TARGETS := x86_64-w64-mingw32.static
MXE_USE_CCACHE := no
```

3. Follow [Step 2 of the Tutorial](https://mxe.cc/#tutorial) to install MXE system-wide.

Scenegraph build scripts assume MXE is installed in the suggested `/opt/mxe`.

4. In the `mxe` root dir, `make cc` to build the cross-compile toolchain. The process will take a while.

5. Once `make cc` completes, follow [Step 4 of the Tutorial](https://mxe.cc/#tutorial), adding the newly-built cross-compile binaries to the beginning of your `PATH`.

6. Now Scenegraph dependencies can be cross-built for Windows.

##### Cross-build Scenegraph Dependencies

Make sure `/opt/mxe/usr/bin` is at the beginning of your shell's `PATH` before proceeding.

Scenegraph depends on the following libraries: `libpng jpeg tiff pthreads freetype`. 

All are included with MXE, and most can be built with no further effort. In the `mxe` root dir:
`make libpng jpeg tiff pthreads`

`freetype` is also included with MXE, but requires additional effort to cross-build on Mac:  
- [Perl-Compatible Regular Expressions](https://www.pcre.org/) must be installed. To install PCRE: `brew install pcre`

- The MXE default arguments to `ar` are insufficient for aarch64-apple-darwin. Edit `src/zlib.mk`, adding `ARFLAGS='rcs'` prior to the `./configure` and `install` calls. The relevant portion of the file should now look like this:

```
[...]
define $(PKG)_BUILD
    cd '$(1)' && CHOST='$(TARGET)' CC='$(PREFIX)/bin/$(TARGET)-gcc' AR='$(PREFIX)/bin/$(TARGET)-ar' RANLIB='$(PREFIX)/bin/$(TARGET)-ranlib' ARFLAGS='rcs' ./configure \
        --prefix='$(PREFIX)/$(TARGET)' \
        --static
    $(MAKE) -C '$(1)' -j '$(JOBS)' CC='$(PREFIX)/bin/$(TARGET)-gcc' AR='$(PREFIX)/bin/$(TARGET)-ar' RANLIB='$(PREFIX)/bin/$(TARGET)-ranlib' ARFLAGS='rcs' install
endef
[...]
```

- Create symlinks to Developer Tools `ar`, `g++`, `gcc`, `ld`, `ranlib` and `strip` in `mxe/usr/bin`, so an `ls -l` includes the following:  
```
lrwxr-xr-x  1 csdfdev  staff  89 May 15 10:56 aarch64-apple-darwin25.0.0-ar -> /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/ar
lrwxr-xr-x  1 csdfdev  staff  54 May 15 10:57 aarch64-apple-darwin25.0.0-g++ -> /Applications/Xcode.app/Contents/Developer/usr/bin/g++
lrwxr-xr-x  1 csdfdev  staff  54 May 15 10:57 aarch64-apple-darwin25.0.0-gcc -> /Applications/Xcode.app/Contents/Developer/usr/bin/gcc
lrwxr-xr-x  1 csdfdev  staff  89 May 15 10:57 aarch64-apple-darwin25.0.0-ld -> /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/ld
lrwxr-xr-x  1 csdfdev  staff  93 May 15 10:57 aarch64-apple-darwin25.0.0-ranlib -> /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/ranlib
lrwxr-xr-x  1 csdfdev  staff  92 May 15 10:57 aarch64-apple-darwin25.0.0-strip -> /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/strip
```

Now `make freetype` should build successfully.

##### (Optional) Cross-build libsquish Dependency
By default, Fast DXT is used for S3 texture generation. No further effort is needed for Fast DXT,
but `libsquish` can be used instead if desired. [Download the libsquish-1.15 source](https://sourceforge.net/projects/libsquish/files/), then:


1. Change directory to the root `libsquish-1.15` source dir
2. `/opt/mxe/usr/bin/x86_64-w64-mingw32.static-cmake .` to generate a `Makefile` in the `libsquish-1.15` dir
3. `make` to build libsquish and place resulting `libsquish.a` in the `libsquish-1.15` dir

Adjust scenegraph/build.gradle to use libsquish instead of Fast DXT:
- comment `-DUSE_FASTDXT` arg in crossCompileWin()
- uncomment `libsquish-1.15` lib arg in crossBuildJNIWin()
- uncomment `-lsquish` and `-lgomp` args in linkCrossCompiledWin()

##### Cross-build Scenegraph DLL

At this point, you're ready to cross-build the Scenegraph DLL for Windows.

In `scenegraph/build.gradle`, examine the paths in the `crossBuildJNIWin`, `crossCompileWin`, and `linkCrossCompiledWin` tasks and adjust them to reflect your MXE and Java Development Kit install paths.

Then, in the `scenegraph` dir, cross-build the DLL with `../gradlew --info crossBuildJNIWin`

The generated `scenegraph.dll` will be placed in the `scenegraph/dist` directory.

#### Native Build with Visual Studio

Microsoft provides [free Community Editions of Visual Studio](https://visualstudio.microsoft.com/vs/community/), allowing developers to build the scenegraph DLL natively without the added expense of the Visual Studio IDE.

This method produces a scenegraph DLL that requires dependent DLLs `pthreadVC2.dll libpng16.dll tiff.dll` in the same
dir as `scenegraph.dll` for `Corelyzer.exe` to launch.

Required headers, libraries, and prebuilt DLLs can be found in `scenegraph/deps/x64`.

##### Build scenegraph in Visual Studio
Open `scenegraph/win32/vstudio/scenegraph.sln` in Visual Studio.

Select the `Project > Properties` menu item to view the project's Property Pages:
In `C/C++ > General`, update the paths in `Additional Include Directories` to those of your installed JDK.
In `Linker > General`, update the paths in `Additional Library Directories` to those of your installed JDK.

Select the Release/x64 configuration and build scenegraph. Ignore the many warnings. (If they really bother you, PRs are welcomed!)

`scenegraph.dll` will be found in the `scenegraph/win32/vstudio/x64/Release` directory.

To use, copy `scenegraph.dll` and the dependent DLLs (`pthreadVC2.dll libpng16.dll tiff.dll`) from `scenegraph/deps/x64` into a dir alongside `Corelyzer.exe`.

If the Debug/x64 configuration is selected, be sure to use the provided debug libpng DLL (`libpng16d.dll`, note the extra `d`) instead of release, or Corelyzer will crash at launch.

### Linux

The following steps result in a working build of Scenegraph on a on Ubuntu LTS 18.04.4 "Bionic Beaver" and 20.04.1 "Focal Fossa".

**NOTE**: Corelyzer is known to crash when launched on Ubuntu virtual machines with kernel version 5.4+. This can be resolved by disabling hardware acceleration in the VM.

Install required dev tools:

    sudo apt install git
    sudo apt install openjdk-11-jdk
    sudo apt install mesa-common-dev libglu1-mesa-dev
    sudo apt install libfreetype6-dev libpng-dev libjpeg-dev libtiff-dev libsquish-dev

Clone the Corelyzer GitHub repo and move to the `scenegraph` dir:

    git clone https://github.com/corewall/corelyzer
    cd scenegraph

In `scenegraph/build.gradle`, modify the Java include paths in the `buildJNILinux` task
to reflect your configuration.

Use the provided Gradle wrapper to build:

    ../gradlew buildJNILinux

This will generate `libscenegraph.so` and `scenegraph-[version].jar` in `scenegraph/dist`.

Follow the instructions in `corelyzer/README.md` to build the Java components of Corelyzer and launch the application.

