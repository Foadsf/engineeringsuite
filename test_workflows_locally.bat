@echo off
setlocal enabledelayedexpansion

echo --- GitHub Actions Local Test Runner ---
set "WORKFLOW_DIR=.github\workflows"
set "LINT_ERRORS=0"
set "EXEC_ERRORS=0"

rem --- Step 1: Check if Workflow Directory Exists ---
if not exist "%WORKFLOW_DIR%" (
    echo ERROR: Workflow directory not found: %WORKFLOW_DIR%
    goto end_summary
)

rem --- Step 2: (Optional) YAML Linting ---
echo.
echo --- Linting YAML Files (Requires yamllint) ---
where yamllint >nul 2>nul
if %errorlevel% == 0 (
    echo Found yamllint. Linting files in %WORKFLOW_DIR%...
    for /F "delims=" %%F in ('dir /b "%WORKFLOW_DIR%\*.yml"') do (
        echo   Linting %%F...
        yamllint "%WORKFLOW_DIR%\%%F"
        if errorlevel 1 (
            echo   ERROR: Linting failed for %%F
            set /a LINT_ERRORS+=1
        ) else (
            echo   Linting passed for %%F
        )
    )
) else (
    echo WARNING: yamllint not found in PATH. Skipping YAML linting.
    echo          Install Python and run 'pip install yamllint' to enable.
)

rem --- Step 3: Execute Local Test Script ---
echo.
echo --- Executing Core Workflow Logic Locally ---
call local_build_test.bat
if errorlevel 1 (
    echo ERROR: Local build/test execution failed.
    set EXEC_ERRORS=1
) else (
    echo Local build/test execution succeeded.
)

rem --- Step 4: Final Summary ---
:end_summary
echo.
echo --- Test Summary ---
if %LINT_ERRORS% GTR 0 (
    echo YAML Linting: FAILED %LINT_ERRORS% errors
) else (
    echo YAML Linting: PASSED or SKIPPED
)
if %EXEC_ERRORS% GTR 0 (
    echo Local Execution: FAILED
) else (
    echo Local Execution: PASSED
)
echo.

if %LINT_ERRORS% GTR 0 (
    goto report_failure
)
if %EXEC_ERRORS% GTR 0 (
    goto report_failure
)

echo ALL LOCAL CHECKS PASSED
endlocal
exit /b 0

:report_failure
echo ONE OR MORE LOCAL CHECKS FAILED. See details above.
endlocal
exit /b 1
