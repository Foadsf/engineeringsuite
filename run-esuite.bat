@echo off
echo Starting Engineering Suite...
echo.
echo NOTE: This application requires Java 8 (JRE or JDK) to be installed
echo and accessible via the system PATH or JAVA_HOME environment variable.
echo If it fails to start, please ensure Java 8 is configured correctly.
echo.

rem Check if JAVA_HOME is set - prioritize it if it seems like Java 8
set "JAVA_CMD=java"
if defined JAVA_HOME (
    echo Found JAVA_HOME: %JAVA_HOME%
    rem Simple check if path contains '8' or '1.8' - adjust if needed for specific installs
    echo %JAVA_HOME% | findstr /I /C:"8" > nul
    if %errorlevel% == 0 (
        echo JAVA_HOME appears to be Java 8, using it.
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    ) else (
       echo JAVA_HOME does not appear to be Java 8, relying on PATH.
    )
) else (
    echo JAVA_HOME not set, relying on PATH for java command.
)

echo.
echo Executing: %JAVA_CMD% -cp ".;bin;Dependencies\*" gui.Principal
echo.

"%JAVA_CMD%" -cp ".;bin;Dependencies\*" gui.Principal

echo.
echo Engineering Suite exited. Press any key to close this window...
pause > nul