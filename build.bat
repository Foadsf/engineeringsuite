@echo off
echo Cleaning previous build...
if exist bin (
    echo Deleting bin directory...
    rd /s /q bin
    if errorlevel 1 (
       echo ERROR: Failed to delete bin directory. Check permissions or if files are in use.
       goto :eof
    )
)
echo Creating bin directory...
mkdir bin
if errorlevel 1 (
    echo ERROR: Failed to create bin directory.
    goto :eof
)

rem --- Define Path to JDK 8 ---
rem Option 1: Rely on JAVA_HOME (Recommended for flexibility)
set "JDK8_HOME=%JAVA_HOME%"
rem Option 2: Hardcode path (Uncomment below and edit if needed, comment out Option 1)
rem set "JDK8_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.442.6-hotspot"

rem --- Check if JDK 8 Path is Found ---
if not defined JDK8_HOME (
    echo ERROR: JAVA_HOME environment variable is not set.
    echo Please set JAVA_HOME to point to your JDK 8 installation directory.
    goto :fail
)
if not exist "%JDK8_HOME%\bin\javac.exe" (
    echo ERROR: javac.exe not found in %JDK8_HOME%\bin.
    echo Please ensure JAVA_HOME points to a valid JDK 8 installation.
    goto :fail
)
echo Using JDK 8 from: %JDK8_HOME%
echo.

echo Generating sources list...
dir /s /b src\*.java > sources.txt

echo Compiling with JDK 8...
"%JDK8_HOME%\bin\javac.exe" -encoding UTF-8 -d bin -cp "Dependencies\*" @sources.txt

if errorlevel 1 (
    echo ********** BUILD FAILED! **********
    del sources.txt
    goto :fail
)

echo ********** BUILD SUCCESSFUL! **********
del sources.txt

:cleanup
rem Add any other cleanup if needed
goto :eof

:fail
echo Build process encountered an error.
pause