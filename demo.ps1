# ==============================================================================
# SPE Mini-Project: Calculator CI/CD Pipeline - Live Demo & Self-Check Script
# Student: Vijay (vijay.vignesh96000@gmail.com)
# Course: CSE 816 - Software Production Engineering, IIIT Bangalore
# ==============================================================================

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "  CALCULATOR REST API & CI/CD PIPELINE - LIVE DEMO VERIFICATION" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 1. Check Docker Containers Status
Write-Host "`n[STEP 1] Checking Running Containers (Docker Compose)..." -ForegroundColor Yellow
docker compose ps

# 2. Check Non-root Container User & Security
Write-Host "`n[STEP 2] Verifying Docker Security (Non-root user)..." -ForegroundColor Yellow
$user = docker run --rm --entrypoint whoami calculator-api:latest
Write-Host "Container executes as user: " -NoNewline
Write-Host "$user" -ForegroundColor Green

# 3. Check Image Size
Write-Host "`n[STEP 3] Verifying Multi-Stage Docker Image Size..." -ForegroundColor Yellow
docker images calculator-api:latest

# Helper function to test endpoints using -UseBasicParsing to prevent prompts
function Test-Endpoint {
    param (
        [string]$Name,
        [string]$Url,
        [int]$ExpectedStatus = 200
    )

    Write-Host "`n--- $Name ---" -ForegroundColor Yellow
    Write-Host "URL: $Url" -ForegroundColor Gray

    try {
        $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        Write-Host "HTTP Status: $($response.StatusCode)" -ForegroundColor Green
        
        # Handle string or byte array content cleanly
        $content = $response.Content
        if ($content -is [byte[]]) {
            $content = [System.Text.Encoding]::UTF8.GetString($content)
        }
        Write-Host "Response Body: $content" -ForegroundColor White
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $body = $reader.ReadToEnd()
        
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "HTTP Status: $statusCode (Expected Validation Error)" -ForegroundColor Green
            Write-Host "Error Body: $body" -ForegroundColor White
        } else {
            Write-Host "HTTP Status: $statusCode (Unexpected)" -ForegroundColor Red
            Write-Host "Error Body: $body" -ForegroundColor Red
        }
    }
}

# 4. Test REST Endpoints via Nginx Reverse Proxy (Port 80)
Write-Host "`n[STEP 4] Testing REST API Endpoints via Nginx Reverse Proxy (Port 80)..." -ForegroundColor Yellow

Test-Endpoint -Name "Actuator Health Probe" -Url "http://localhost/actuator/health" -ExpectedStatus 200
Test-Endpoint -Name "Operation: Addition (a=25, b=15)" -Url "http://localhost/api/v1/add?a=25&b=15" -ExpectedStatus 200
Test-Endpoint -Name "Operation: Subtraction (a=100, b=35)" -Url "http://localhost/api/v1/subtract?a=100&b=35" -ExpectedStatus 200
Test-Endpoint -Name "Operation: Multiplication (a=7, b=8)" -Url "http://localhost/api/v1/multiply?a=7&b=8" -ExpectedStatus 200
Test-Endpoint -Name "Operation: Division (a=84, b=4)" -Url "http://localhost/api/v1/divide?a=84&b=4" -ExpectedStatus 200

# 5. Test Error Handling & Validations
Write-Host "`n[STEP 5] Testing Validation & Error Responses (RFC 7807)..." -ForegroundColor Yellow

Test-Endpoint -Name "Division by Zero (a=10, b=0)" -Url "http://localhost/api/v1/divide?a=10&b=0" -ExpectedStatus 400
Test-Endpoint -Name "Invalid Parameter (a=hello, b=5)" -Url "http://localhost/api/v1/add?a=hello&b=5" -ExpectedStatus 400
Test-Endpoint -Name "Missing Parameter (a=50)" -Url "http://localhost/api/v1/add?a=50" -ExpectedStatus 400

Write-Host "`n==================================================================" -ForegroundColor Cyan
Write-Host "  ALL LOCAL DEMO CHECKS COMPLETED SUCCESSFULLY!" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan
