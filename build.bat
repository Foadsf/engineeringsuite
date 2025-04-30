@echo off
setlocal enabledelayedexpansion

echo --- Engineering Suite Build Script ---
echo.

rem --- Configuration ---
set "JDK8_PATH=C:\Program Files\Eclipse Adoptium\jdk-8.0.442.6-hotspot"
rem set "JDK8_PATH=%JAVA_HOME%"

rem --- Pre-Checks ---
echo [Step 1/7] Verifying JDK 8 path...
if not defined JDK8_PATH (
    echo ERROR: JDK8_PATH is not set in the script.
    goto fail
)
if not exist "%JDK8_PATH%" (
    echo ERROR: JDK 8 directory not found: %JDK8_PATH%
    goto fail
)
if not exist "%JDK8_PATH%\bin\javac.exe" (
    echo ERROR: javac.exe not found in %JDK8_PATH%\bin
    goto fail
)
echo      Using JDK 8: %JDK8_PATH%
echo.

echo [Step 2/7] Verifying 'src' directory...
if exist src (
    echo      'src' directory found.
) else (
    echo ERROR: 'src' directory not found in current path: %cd%
    echo        Please run this script from the project root directory.
    goto fail
)
echo.

echo [Step 3/7] Verifying 'Dependencies' directory...
if exist Dependencies (
    echo      'Dependencies' directory found.
) else (
    echo ERROR: 'Dependencies' directory not found in current path: %cd%
    goto fail
)
echo.

echo [Step 4/7] Cleaning previous build (if exists)...
if exist bin (
    echo      Attempting to delete existing 'bin' directory...
    rd /s /q bin
    if errorlevel 1 (
       echo ERROR: Failed to delete 'bin' directory. Possible lock or permissions issue.
       goto fail
    )
    echo      'bin' directory deleted successfully.
) else (
    echo      'bin' directory not found, no cleanup needed.
)
echo.

echo [Step 5/7] Creating 'bin' directory...
mkdir bin
if errorlevel 1 (
    echo ERROR: Failed to create 'bin' directory. Check permissions.
    goto fail
)
echo      'bin' directory created.
echo.

echo [Step 6/7] Generating source file list...
dir /s /b src\*.java > sources.txt
if errorlevel 1 (
    echo ERROR: Failed to list source files from 'src'.
    goto fail
)
for %%F in (sources.txt) do if %%~zF == 0 (
    echo ERROR: No .java files found in 'src' or subdirectories.
    del sources.txt
    goto fail
)
echo      sources.txt created.
echo.

echo [Step 7/7] Compiling with JDK 8...
"%JDK8_PATH%\bin\javac.exe" -encoding UTF-8 -d bin -cp "Dependencies\*" @sources.txt
if errorlevel 1 (
    echo.
    echo ********** BUILD FAILED! **********
    del sources.txt
    goto fail
)
echo      Compilation successful.
echo.
echo ********** BUILD COMPLETED SUCCESSFULLY **********
goto cleanup

:fail
echo.
echo ********** BUILD PROCESS FAILED **********
if exist sources.txt del sources.txt
echo Press any key to exit...
pause > nul
exit /b 1

:cleanup
if exist sources.txt del sources.txt
echo Build script finished.
endlocal
exit /b 0