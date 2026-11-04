<#
.SYNOPSIS
    Secure, Privacy-Guaranteed Steam Workshop Publisher for Songs of Syx Mods.

.DESCRIPTION
    Builds and packages 'Syx Auto-Planner' and 'Syx Architect' for publication to the Steam Workshop.
    - Dynamically generates .vdf descriptors from templates using your local %APPDATA% path.
    - NEVER commits usernames, Steam IDs, tokens, or credentials to Git.
    - Stores published Workshop IDs locally in a gitignored 'workshop.properties' file.

.PARAMETER Mod
    Which mod to upload: 'All', 'AutoPlanner', or 'Architect'. Default is 'All'.

.PARAMETER SteamUser
    Your Steam login username (not password!). If omitted in interactive mode, prompts for it.

.PARAMETER Visibility
    0 = Public, 1 = Friends Only, 2 = Private / Hidden (Default: 2 for safe initial review).

.PARAMETER ChangeNote
    Workshop changelog entry (Default: "Initial Release v1.0.0").

.PARAMETER DryRun
    If set, only generates and verifies the local .vdf descriptor files without contacting Steam.
#>

[CmdletBinding()]
param(
    [ValidateSet("All", "AutoPlanner", "Architect")]
    [string]$Mod = "All",

    [string]$SteamUser = "",

    [ValidateSet(0, 1, 2)]
    [int]$Visibility = 2,

    [string]$ChangeNote = "Initial Release v1.0.0",

    [string]$SteamCmdPath = "",

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
$GeneratedDir = Join-Path $ScriptDir "generated"
$TemplatesDir = Join-Path $ScriptDir "templates"
$PropsFile = Join-Path $ScriptDir "workshop.properties"

# Ensure generated directory exists
if (-not (Test-Path $GeneratedDir)) {
    New-Item -ItemType Directory -Path $GeneratedDir -Force | Out-Null
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "    Songs of Syx (v0.71.44) - Steam Workshop Deployment Tool    " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# 1. Load existing workshop IDs from local gitignored properties file if present
$WorkshopIds = @{
    "AutoPlanner" = "0"
    "Architect"   = "0"
}

if (Test-Path $PropsFile) {
    Write-Host "[INFO] Reading existing Workshop IDs from: $PropsFile" -ForegroundColor Gray
    Get-Content $PropsFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line.Split("=", 2)
            if ($parts.Length -eq 2) {
                $k = $parts[0].Trim()
                $v = $parts[1].Trim()
                if ($k -eq "AUTO_PLANNER_WORKSHOP_ID") { $WorkshopIds["AutoPlanner"] = $v }
                if ($k -eq "ARCHITECT_WORKSHOP_ID")    { $WorkshopIds["Architect"]   = $v }
            }
        }
    }
}

# 2. Define Mod Targets
$AppData = [System.Environment]::GetFolderPath([System.Environment+SpecialFolder]::ApplicationData)
$SyxModsDir = Join-Path $AppData "songsofsyx\mods"

$Targets = @()
if ($Mod -eq "All" -or $Mod -eq "AutoPlanner") {
    $Targets += [PSCustomObject]@{
        Key           = "AutoPlanner"
        DisplayName   = "Syx Auto-Planner"
        TemplateName  = "SyxAutoPlanner.template.vdf"
        GeneratedVdf  = (Join-Path $GeneratedDir "SyxAutoPlanner.vdf")
        ContentFolder = (Join-Path $SyxModsDir "SyxAutoPlanner")
        PreviewFile   = (Join-Path $SyxModsDir "SyxAutoPlanner\icon.png")
        CurrentId     = $WorkshopIds["AutoPlanner"]
    }
}
if ($Mod -eq "All" -or $Mod -eq "Architect") {
    $Targets += [PSCustomObject]@{
        Key           = "Architect"
        DisplayName   = "Syx Architect"
        TemplateName  = "SyxArchitect.template.vdf"
        GeneratedVdf  = (Join-Path $GeneratedDir "SyxArchitect.vdf")
        ContentFolder = (Join-Path $SyxModsDir "SyxArchitect")
        PreviewFile   = (Join-Path $SyxModsDir "SyxArchitect\icon.png")
        CurrentId     = $WorkshopIds["Architect"]
    }
}

# 3. Verify Local Mod Builds
foreach ($target in $Targets) {
    $infoFile = Join-Path $target.ContentFolder "_Info.txt"
    $iconFile = $target.PreviewFile
    if (-not (Test-Path $infoFile) -or -not (Test-Path $iconFile)) {
        Write-Warning "Mod directory incomplete for '$($target.DisplayName)' in: $($target.ContentFolder)"
        Write-Host "Running './gradlew deployAll' to compile and deploy latest build..." -ForegroundColor Yellow
        Push-Location $RepoRoot
        try {
            & .\gradlew deployAll
        } finally {
            Pop-Location
        }
        break
    }
}

# 4. Generate .VDF descriptor files dynamically
foreach ($target in $Targets) {
    $templatePath = Join-Path $TemplatesDir $target.TemplateName
    if (-not (Test-Path $templatePath)) {
        throw "Template not found: $templatePath"
    }

    $escapedContent = $target.ContentFolder.Replace("\", "\\")
    $escapedPreview = $target.PreviewFile.Replace("\", "\\")

    $content = Get-Content $templatePath -Raw
    $content = $content.Replace("##PUBLISHED_FILE_ID##", $target.CurrentId)
    $content = $content.Replace("##CONTENT_FOLDER##", $escapedContent)
    $content = $content.Replace("##PREVIEW_FILE##", $escapedPreview)
    $content = $content.Replace("##VISIBILITY##", $Visibility.ToString())
    $content = $content.Replace("##CHANGENOTE##", $ChangeNote)

    [System.IO.File]::WriteAllText($target.GeneratedVdf, $content, [System.Text.Encoding]::UTF8)
    Write-Host "[OK] Generated VDF for $($target.DisplayName):" -ForegroundColor Green
    Write-Host "     File: $($target.GeneratedVdf)" -ForegroundColor DarkGray
    Write-Host "     Target Folder: $($target.ContentFolder)" -ForegroundColor DarkGray
    Write-Host "     PublishedFileID: $($target.CurrentId) $(if ($target.CurrentId -eq '0') {'(NEW UPLOAD)'} else {'(UPDATE)'})" -ForegroundColor DarkGray
}

if ($DryRun) {
    Write-Host "`n[DRY RUN COMPLETE] Descriptor files generated successfully in 'steam/generated/'." -ForegroundColor Yellow
    Write-Host "No upload was attempted. All local paths and tokens are strictly gitignored." -ForegroundColor Yellow
    exit 0
}

# 5. Locate SteamCMD
if (-not $SteamCmdPath) {
    $found = Get-Command "steamcmd" -ErrorAction SilentlyContinue
    if ($found) {
        $SteamCmdPath = $found.Source
    } elseif (Test-Path "C:\steamcmd\steamcmd.exe") {
        $SteamCmdPath = "C:\steamcmd\steamcmd.exe"
    } elseif (Test-Path "$env:ProgramFiles (x86)\Steam\steamcmd.exe") {
        $SteamCmdPath = "$env:ProgramFiles (x86)\Steam\steamcmd.exe"
    } else {
        Write-Host "`n[!] SteamCMD executable not detected in PATH or C:\steamcmd\." -ForegroundColor Yellow
        $SteamCmdPath = Read-Host "Please enter full path to steamcmd.exe"
    }
}

if (-not (Test-Path $SteamCmdPath)) {
    throw "Cannot proceed: SteamCMD not found at '$SteamCmdPath'. Run with -DryRun to only generate VDF files."
}

# 6. Prompt for Steam username if not provided
if (-not $SteamUser) {
    Write-Host "`nEnter your Steam account username to authenticate with Steam Workshop:" -ForegroundColor White
    $SteamUser = Read-Host "Steam Username"
}

if (-not $SteamUser) {
    throw "Steam username is required to upload."
}

# 7. Execute Uploads
Write-Host "`nReady to upload to Steam Workshop." -ForegroundColor Cyan
Write-Host "SteamCMD will now launch. You will be prompted for your password and Steam Guard code by Valve directly." -ForegroundColor Yellow
Write-Host "(Your credentials are never intercepted, passed to scripts, or saved to disk).`n" -ForegroundColor Yellow

foreach ($target in $Targets) {
    Write-Host "--> Uploading '$($target.DisplayName)'..." -ForegroundColor Cyan
    $argsList = @(
        "+login", $SteamUser,
        "+workshop_build_item", "`"$($target.GeneratedVdf)`"",
        "+quit"
    )

    $proc = Start-Process -FilePath $SteamCmdPath -ArgumentList $argsList -NoNewWindow -Wait -PassThru

    if ($proc.ExitCode -ne 0) {
        Write-Warning "SteamCMD exited with code $($proc.ExitCode) for $($target.DisplayName)."
    } else {
        Write-Host "[SUCCESS] Upload command finished for $($target.DisplayName)!" -ForegroundColor Green
    }
}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "Upload process complete." -ForegroundColor Cyan
Write-Host "If Steam assigned a new PublishedFileID, save it in 'steam/workshop.properties':" -ForegroundColor White
Write-Host "  AUTO_PLANNER_WORKSHOP_ID=<ID>" -ForegroundColor Gray
Write-Host "  ARCHITECT_WORKSHOP_ID=<ID>" -ForegroundColor Gray
Write-Host "=================================================================" -ForegroundColor Cyan
