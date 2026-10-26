param(
    [string]$StagingDirectory = (Join-Path $PSScriptRoot '..\target\cd-simulation')
)

$ErrorActionPreference = 'Stop'
$projectDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$wrapper = Join-Path $projectDirectory 'mvnw.cmd'

Push-Location $projectDirectory
try {
    & $wrapper --batch-mode clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven verify failed with exit code $LASTEXITCODE."
    }

    $jar = Join-Path $projectDirectory 'target\blog-api.jar'
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
        throw "Expected build artifact was not created: $jar"
    }

    $resolvedStaging = [System.IO.Path]::GetFullPath($StagingDirectory)
    New-Item -ItemType Directory -Force -Path $resolvedStaging | Out-Null
    $stagedJar = Join-Path $resolvedStaging 'blog-api.jar'
    Copy-Item -LiteralPath $jar -Destination $stagedJar -Force
    if ((Get-Item -LiteralPath $stagedJar).Length -le 0) {
        throw 'The staged application artifact is empty.'
    }
    Write-Host "Deployment simulation staged the tested JAR at $stagedJar. No external service was contacted."
}
finally {
    Pop-Location
}
