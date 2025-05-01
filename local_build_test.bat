@echo off
setlocal enabledelayedexpansion

echo --- Executing Local Build and Test Sequence ---
echo.

rem --- Configuration (Match your build script) ---
set "JDK8_PATH=C:\Program Files\Eclipse Adoptium\jdk-8.0.442.6-hotspot"
set "JAVAC_CMD=%JDK8_PATH%\bin\javac.exe"
set "JAVA_CMD=%JDK8_PATH%\bin\java.exe"
set "EXAMPLES_DIR=examples"
set "CLI_SCRIPT=run_cli.bat"

rem --- Pre-Checks (Simplified for local) ---
echo [1/5] Checking JDK...
if not exist "%JAVAC_CMD%" (
    echo ERROR: Local JDK 8 javac not found at %JDK8_PATH%\bin\javac.exe
    goto fail
)
if not exist "%JAVA_CMD%" (
    echo ERROR: Local JDK 8 java not found at %JDK8_PATH%\bin\java.exe
    goto fail
)
echo      JDK 8 found locally.
echo.

echo [2/5] Cleaning previous build...
if exist bin (
    echo      Deleting existing 'bin' directory...
    rd /s /q bin
    if errorlevel 1 ( echo ERROR: Failed to delete 'bin'.& goto fail )
)
mkdir bin
if errorlevel 1 ( echo ERROR: Failed to create 'bin'.& goto fail )
echo      Cleaned and created 'bin'.
echo.

echo [3/5] Compiling source code...
echo      Generating sources list...
dir /s /b src\*.java > sources.txt
if errorlevel 1 ( echo ERROR: Failed to list sources.& goto fail )
echo      Compiling...
"%JAVAC_CMD%" -encoding UTF-8 -d bin -cp "Dependencies\*" @sources.txt
if errorlevel 1 (
    echo ERROR: Compilation failed.
    del sources.txt
    goto fail
)
del sources.txt
echo      Compilation successful.
echo.

echo [4/5] Running Example Tests...
set "TEST_FAILURES=0"
rem Exclude list (Add known problematic files here)
set "EXCLUDE_LIST=10_SymbolicAndNumerical.md 13_SymbolicSimplification.md 07_PlottingExamples.md 09_ImplicitFrictionFactor.ris 12_ChemicalEquilibrium.ris 22_SimpleOptimization.ris"

for /F "delims=" %%F in ('dir /b "%EXAMPLES_DIR%\*.ris"') do (
    set "FILENAME=%%F"
    set "EXCLUDED=0"
    REM Check against exclude list
    for %%X in (%EXCLUDE_LIST%) do (
        if /I "!FILENAME!"=="%%X" (
            set "EXCLUDED=1"
        )
    )

    if !EXCLUDED! == 1 (
        echo   Skipping excluded %%F...
    ) else (
        echo   Testing %%F...
        call %CLI_SCRIPT% "%EXAMPLES_DIR%\%%F" /test
        if errorlevel 1 (
            echo   ERROR: Test failed for %%F
            set /a TEST_FAILURES+=1
            rem Optional: Stop on first failure: goto test_summary
        ) else (
            echo   Test passed for %%F
        )
        echo.
    )
)

:test_summary
echo.
echo [5/5] Test Summary:
if %TEST_FAILURES% GTR 0 (
    echo   %TEST_FAILURES% test(s) FAILED.
    goto fail
) else (
    echo   All executed tests PASSED.
)
echo.
echo --- Local Build and Test Sequence SUCCEEDED ---
goto success

:fail
echo.
echo --- Local Build & Test Sequence FAILED ---
endlocal
exit /b 1

:success
endlocal
exit /b 0
