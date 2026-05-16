param(
    [string]$SonarToken = $env:SONAR_TOKEN,
    [string]$SonarOrganization = "abhi-8279",
    [string]$SonarHostUrl = "https://sonarcloud.io"
)

$ErrorActionPreference = "Stop"

if (-not $SonarToken) {
    throw "Provide a SonarCloud token with -SonarToken or set SONAR_TOKEN in the environment."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$mavenImage = "maven:3.9.9-eclipse-temurin-21"
$scannerImage = "sonarsource/sonar-scanner-cli:latest"
$services = @(
    "api-gateway",
    "appointment-service",
    "auth-service",
    "eureka-server",
    "notification-service",
    "payment-service",
    "provider-service",
    "record-service",
    "review-service",
    "schedule-service"
)

foreach ($service in $services) {
    Write-Host "==> Building $service and generating test coverage"
    docker run --rm `
        -v "${repoRoot}:/workspace" `
        -w "/workspace/$service" `
        $mavenImage `
        mvn clean verify

    if ($LASTEXITCODE -ne 0) {
        throw "Build failed for $service"
    }
}

Write-Host "==> Running SonarCloud scan for backend"
docker run --rm `
    -v "${repoRoot}:/usr/src" `
    -w /usr/src `
    $scannerImage `
    -Dsonar.host.url=$SonarHostUrl `
    -Dsonar.organization=$SonarOrganization `
    -Dsonar.token=$SonarToken

if ($LASTEXITCODE -ne 0) {
    throw "SonarCloud scan failed"
}

Write-Host "==> Backend SonarCloud scan completed"
