@echo off
echo Starting Engineering Suite...
echo.
echo NOTE: This application requires Java 8 (JRE or JDK) to be installed.
echo Trying to use recommended JDK 8 path first...
echo.

set "PREFERRED_JDK8=C:\Program Files\Eclipse Adoptium\jdk-8.0.442.6-hotspot"
set "JAVA_CMD=java"

if exist "%PREFERRED_JDK8%\bin\java.exe" (
    echo Found recommended JDK 8: %PREFERRED_JDK8%
    set "JAVA_CMD=%PREFERRED_JDK8%\bin\java.exe"
) else if defined JAVA_HOME (
    echo Recommended JDK 8 not found. Checking JAVA_HOME: %JAVA_HOME%
    rem Check if JAVA_HOME path looks like Java 8
    echo %JAVA_HOME% | findstr /I /R /C:"\\jdk-8" /C:"\\jdk1\.8" > nul
    if %errorlevel% == 0 (
        echo JAVA_HOME appears to be Java 8, using it.
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    ) else (
        echo JAVA_HOME does not appear to be Java 8. Relying on system PATH for 'java'.
        echo Ensure 'java' in PATH points to a Java 8 installation.
    )
) else (
    echo Recommended JDK 8 not found and JAVA_HOME not set. Relying on system PATH for 'java'.
    echo Ensure 'java' in PATH points to a Java 8 installation.
)

echo.
echo Executing: "%JAVA_CMD%" -cp ".;bin;Dependencies\*" gui.Principal
echo.

"%JAVA_CMD%" -cp ".;bin;Dependencies\*" gui.Principal

echo.
echo Engineering Suite exited. Press any key to close this window...
pause > nul