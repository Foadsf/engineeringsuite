#Requires -RunAsAdministrator

$ErrorActionPreference = 'Stop' # Stop on errors

$packageName = $env:ChocolateyPackageName
$packageVersion = $env:ChocolateyPackageVersion
$toolsDir = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$zipFilePath = Join-Path $toolsDir 'eSuite-v1.0.2.zip'

# Safeguard for installDir
if ([string]::IsNullOrEmpty($env:ChocolateyToolsLocation)) {
    Write-Warning "ChocolateyToolsLocation environment variable is empty or null! Defaulting to C:\tools"
    $effectiveToolsLocation = "C:\tools"
} else {
    $effectiveToolsLocation = $env:ChocolateyToolsLocation
}
$installDir = Join-Path $effectiveToolsLocation "$packageName"

$shortcutName = "Engineering Suite ${packageVersion}"
$shortcutLink = Join-Path $env:ProgramData "Microsoft\Windows\Start Menu\Programs\$shortcutName.lnk"
$shortcutTarget = Join-Path $installDir 'run-esuite.bat'
$shortcutIcon = Join-Path $installDir 'icons\logo.png'

Write-Host "Installing $packageName version $packageVersion"

# --- Replacement for Install-ChocolateyZipPackage ---
Write-Host "Ensuring installation directory exists: $installDir"
# Ensure the destination directory exists before unzipping
New-Item -Path $installDir -ItemType Directory -Force | Out-Null

Write-Host "Unzipping '$zipFilePath' to '$installDir'..."
# Use the core unzip helper
Get-ChocolateyUnzip -FileFullPath $zipFilePath -Destination $installDir
# --- End Replacement ---

# Create Start Menu Shortcut
Write-Host "Creating Start Menu shortcut: $shortcutLink"
Install-ChocolateyShortcut -shortcutFilePath $shortcutLink -targetPath $shortcutTarget -IconLocation $shortcutIcon -WorkingDirectory $installDir

Write-Host "$packageName installation complete."