param(
    [string]$RepoPath = "."
)

$ErrorActionPreference = "Stop"

Push-Location $RepoPath
try {
    $failureCount = 0

    Write-Host "==> Checking known sensitive paths in reachable history"
    $objects = git rev-list --objects --all
    $checks = [ordered]@{
        "bad_jar_path_refs" = "ubuntu@www\.sku-graduation\.p-e\.kr"
        "application_properties_refs" = "src/main/resources/application\.properties"
        "teamPost_refs" = " teamPost(/|$)"
    }

    foreach ($name in $checks.Keys) {
        $count = (($objects | Select-String -Pattern $checks[$name]) | Measure-Object).Count
        Write-Host "$name=$count"
        if ($count -gt 0) {
            $failureCount += $count
        }
    }

    Write-Host ""
    Write-Host "==> Checking common AWS credential patterns in reachable text files"
    $awsPattern = "\b(AKIA|ASIA|AGPA|AIDA|AROA|AIPA|ANPA)[0-9A-Z]{16}\b"
    $awsMatches = git grep -nI -E $awsPattern $(git rev-list --all) -- . 2>$null
    $awsMatchCount = (($awsMatches) | Measure-Object).Count
    Write-Host "aws_access_key_pattern_refs=$awsMatchCount"
    if ($awsMatchCount -gt 0) {
        $failureCount += $awsMatchCount
    }

    Write-Host ""
    Write-Host "==> Checking current working tree with local patterns"
    $currentMatches = rg --hidden --glob "!/.git/**" --glob "!build/**" --glob "!.gradle/**" -n -i `
        --glob "!scripts/security/**" --glob "!SECURITY_PUBLIC_RELEASE.md" `
        "(AKIA|ASIA|AWS_ACCESS_KEY|AWS_SECRET_KEY|aws_secret_access_key|aws_access_key_id|BEGIN .*PRIVATE KEY|System\.out\.println|printStackTrace|MYSQL_ROOT_PASSWORD:\s*[^$]|SPRING_DATASOURCE_PASSWORD:\s*[^$])" . 2>$null
    if ($LASTEXITCODE -eq 0) {
        $currentMatches
        $failureCount += (($currentMatches) | Measure-Object).Count
    } else {
        Write-Host "current_tree_pattern_matches=0"
    }

    if ($failureCount -gt 0) {
        throw "Sensitive content scan failed with $failureCount finding(s)."
    }
}
finally {
    Pop-Location
}
