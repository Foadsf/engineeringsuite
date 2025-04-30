@echo off
setlocal enabledelayedexpansion

rem --- Script Configuration ---
set "SCRIPT_NAME=%~n0"
set "PREFERRED_JDK8=C:\Program Files\Eclipse Adoptium\jdk-8.0.442.6-hotspot"
set "MAIN_CLASS=cli.CommandLineRunner"
set "CLASSPATH=.;bin;Dependencies\*"

rem --- Title ---
title Engineering Suite CLI Runner

rem --- Argument Check ---
echo Running %SCRIPT_NAME%.bat...
if "%~1"=="" (
    echo ERROR: No .ris file specified.
    echo.
    echo Usage: %SCRIPT_NAME%.bat ^<path_to_your_file.ris^>
    echo Example: %SCRIPT_NAME%.bat examples\08_HeatExchangerLMTD.ris
    goto fail_exit
)
set "RIS_FILE=%~1"
if not exist "%RIS_FILE%" (
    echo ERROR: Input file not found: %RIS_FILE%
    goto fail_exit
)
echo Input file: %RIS_FILE%
echo.

rem --- Java 8 Check ---
echo Locating Java 8 Runtime...
set "JAVA_CMD=java" REM Default to PATH

if exist "%PREFERRED_JDK8%\bin\java.exe" (
    echo   Found recommended JDK 8: %PREFERRED_JDK8%
    set "JAVA_CMD=%PREFERRED_JDK8%\bin\java.exe"
) else if defined JAVA_HOME (
    echo   Recommended JDK 8 not found. Checking JAVA_HOME: %JAVA_HOME%
    echo %JAVA_HOME% | findstr /I /R /C:"\\jdk-8" /C:"\\jdk1\.8" > nul
    if %errorlevel% == 0 (
        echo   JAVA_HOME appears to be Java 8, using it.
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    ) else (
        echo   WARNING: JAVA_HOME does not appear to be Java 8.
        echo            Relying on system PATH for 'java'.
        echo            Ensure 'java' in PATH points to a Java 8 installation!
    )
) else (
    echo   WARNING: Recommended JDK 8 not found and JAVA_HOME not set.
    echo            Relying on system PATH for 'java'.
    echo            Ensure 'java' in PATH points to a Java 8 installation!
)
echo   Using Java command: "%JAVA_CMD%"
echo.

rem --- Check if necessary directories/files exist ---
if not exist bin ( echo ERROR: 'bin' directory not found. Please run build.bat first.& goto fail_exit )
if not exist Dependencies ( echo ERROR: 'Dependencies' directory not found.& goto fail_exit )

rem --- Execute ---
echo Running Engineering Suite CLI...
echo Command: "%JAVA_CMD%" -cp "%CLASSPATH%" %MAIN_CLASS% "%RIS_FILE%"
echo ------------------------------------
echo.

"%JAVA_CMD%" -cp "%CLASSPATH%" %MAIN_CLASS% "%RIS_FILE%"
set "JAVA_EXIT_CODE=%errorlevel%"

echo.
echo ------------------------------------

if %JAVA_EXIT_CODE% == 0 (
    echo Engineering Suite CLI finished successfully.
) else (
    echo ERROR: Engineering Suite CLI finished with errors (Exit Code: %JAVA_EXIT_CODE%).
    echo Please check the output above for details.
)
echo.
goto cleanup_exit

:fail_exit
echo Script aborted due to error.
set JAVA_EXIT_CODE=1

:cleanup_exit
echo Press any key to close this window...
pause > nul
endlocal
exit /b %JAVA_EXIT_CODE%