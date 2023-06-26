corelyzer/packages/win README
June 26 2023

Corelyzer.exe is prepared with the [WinRun4J utility](https://winrun4j.sourceforge.net/) using the provided Corelyzer.ini file, which assumes the following:

- A complete Windows Java 11 runtime environment is provided in a `jre` directory alongside Corelyzer.exe
- All JAR files are found in a `lib` directory alongside Corelyzer.exe
- The main class is corelyzer.ui.splashscreen.SplashScreenMain


To create a new Corelyzer.exe with WinRun4J, first make a copy of WinRun4J64.exe and name it Corelyzer.exe.

Then, from the command-line:

`RCEDIT64.exe /C Corelyzer.exe` to clear existing resources
`RCEDIT64.exe /N Corelyzer.exe Corelyzer.ini` to use settings in the INI file
`RCEDIT64.exe /I Corelyzer.exe corelyzer.ico` to use the Corelyzer icon

The resulting Corelyzer.exe should be ready for use in a properly packaged Windows build.