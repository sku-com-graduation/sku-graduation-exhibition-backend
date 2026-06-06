param(
    [string]$RepoUrl = "https://github.com/sku-com-graduation/sku-graduation-exhibition-backend.git",
    [string]$MirrorDir = (Join-Path $env:TEMP "sku-graduation-exhibition-backend-clean.git"),
    [switch]$Push
)

$ErrorActionPreference = "Stop"

$sensitivePaths = @(
    "ubuntu@www.sku-graduation.p-e.kr",
    "src/main/resources/application.properties",
    "teamPost"
)

function Write-Step($Message) {
    Write-Host ""
    Write-Host "==> $Message"
}

Write-Step "Preparing git-filter-repo"
$toolDir = Join-Path $env:TEMP "codex-git-filter-repo-venv"
if (-not (Test-Path $toolDir)) {
    python -m venv $toolDir
}
$python = Join-Path $toolDir "Scripts\python.exe"
& $python -m pip install --quiet --upgrade pip git-filter-repo

Write-Step "Creating fresh mirror clone"
if (Test-Path $MirrorDir) {
    Remove-Item -LiteralPath $MirrorDir -Recurse -Force
}
git clone --mirror $RepoUrl $MirrorDir

Push-Location $MirrorDir
try {
    Write-Step "Removing sensitive paths from all fetched branches and tags"
    $filterArgs = @("-m", "git_filter_repo")
    foreach ($path in $sensitivePaths) {
        $filterArgs += @("--path", $path)
    }
    $filterArgs += @("--invert-paths", "--force")
    & $python @filterArgs

    Write-Step "Restoring origin remote"
    git remote add origin $RepoUrl

    Write-Step "Validating cleaned history"
    $objects = git rev-list --objects --all
    $badJarPathCount = (($objects | Select-String -Pattern "ubuntu@www\.sku-graduation\.p-e\.kr") | Measure-Object).Count
    $applicationPropertiesCount = (($objects | Select-String -Pattern "src/main/resources/application\.properties") | Measure-Object).Count
    $teamPostCount = (($objects | Select-String -Pattern " teamPost(/|$)") | Measure-Object).Count
    $awsAccessKeyPatternCount = ((git grep -nI -E "\b(AKIA|ASIA|AGPA|AIDA|AROA|AIPA|ANPA)[0-9A-Z]{16}\b" $(git rev-list --all) -- . 2>$null) | Measure-Object).Count

    Write-Host "bad_jar_path_refs=$badJarPathCount"
    Write-Host "application_properties_refs=$applicationPropertiesCount"
    Write-Host "teamPost_refs=$teamPostCount"
    Write-Host "aws_access_key_pattern_refs=$awsAccessKeyPatternCount"

    if ($badJarPathCount -ne 0 -or $applicationPropertiesCount -ne 0 -or $teamPostCount -ne 0 -or $awsAccessKeyPatternCount -ne 0) {
        throw "Cleaned mirror still contains sensitive history. Do not push."
    }

    if ($Push) {
        Write-Step "Force-pushing rewritten history"
        git push --force origin "refs/heads/*:refs/heads/*"
        git push --force origin "refs/tags/*:refs/tags/*"
    } else {
        Write-Step "Dry run complete"
        Write-Host "No push was performed. Re-run with -Push only after collaborators are ready."
        Write-Host "Clean mirror path: $MirrorDir"
    }
}
finally {
    Pop-Location
}
