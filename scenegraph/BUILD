Building the Corelyzer SceneGraph Library

The Corelyzer Scenegraph Library uses native code so requires some initial 
setup to build completely from source.  We currently provide pre-built 
versions of the library so you will only need to follow these instructions if
you intend to change the native code.

These instructions assume you are building on a Mac OS X 10.5 or 10.6 machine
with XCode / Developers Tools already installed.


= Step 1: Install MacPorts =
The MingW cross compiler environment which we are using to generate Windows 
DLLs requires 'wget' and 'gsed' which are available from MacPorts.

Install MacPorts from: http://www.macports.org/install.php
Install 'wget' and 'gsed' via the Terminal:

  $ sudo port install wget gsed


= Step 2: Bootstrap MingW environment =
To build the Windows DLL we will be using the MingW cross compiler.  You will
need to bootstrap it for your machine via the Terminal:

  $ cd scenegraph/deps/win32
  $ tar xzvf bootstrap.tar.gz
  $ cd bootstrap
  $ make libpng jpeg tiff glew pthreads freetype

This will take several minutes so be patient.  It only needs to be done once.


= Step 3: Build the JNI library =
The final step is to build the SceneGraph JNI Library.  You can do this by 
invoking Gradle in the scenegraph/ directory via the Terminal:

  $ ../gradlew clean build-jni

The resulting JAR file, jnilib, and DLLs will be in scenegraph/dist.  You only
need to re-run this step when you change the native code or when you change
the version number.