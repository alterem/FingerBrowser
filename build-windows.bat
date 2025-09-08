@echo off
echo Building FingerprintBrowser for Windows...
mvn clean javafx:jlink
if %ERRORLEVEL% neq 0 (
    echo Failed to create runtime image
    exit /b 1
)

mvn jpackage:jpackage@win
if %ERRORLEVEL% neq 0 (
    echo Failed to create Windows installer
    exit /b 1
)

echo Build completed successfully!
echo Check target/dist/ for the installer
pause