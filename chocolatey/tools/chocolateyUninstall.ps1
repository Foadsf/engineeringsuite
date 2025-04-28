#Requires -RunAsAdministrator

$ErrorActionPreference = 'Stop'; # Stop on errors
$packageName = $env:ChocolateyPackageName
$packageVersion = $env:ChocolateyPackageVersion
$shortcutName = "Engineering Suite ${packageVersion}"
$shortcutLink = Join-Path $env:ProgramData "Microsoft\Windows\Start Menu\Programs\$shortcutName.lnk"

Write-Host "Uninstalling $packageName..."

# Remove Start Menu shortcut directly using PowerShell
Write-Host "Attempting to remove Start Menu shortcut: $shortcutLink"
# Use -Force to attempt removal even if hidden/read-only (unlikely)
# Use -ErrorAction SilentlyContinue to prevent script failure if the shortcut doesn't exist
if (Test-Path -Path $shortcutLink -PathType Leaf) {
    Remove-Item -Path $shortcutLink -Force -ErrorAction SilentlyContinue
    Write-Host "Shortcut removed (if it existed)."
} else {
    Write-Host "Shortcut did not exist."
}

# Note: Files extracted by Install-ChocolateyZipPackage are automatically removed
# by Chocolatey during uninstall based on a tracking file it creates.
# No need to manually delete $installDir here.

Write-Host "$packageName uninstallation complete."