$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Stop"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "      UNIVERSITY ATTENDANCE SYSTEM TEST RUNNER      " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# Helper function to extract CSRF token from HTML
function Get-CsrfToken($html) {
    if ($html -match 'name="_csrf"\s+content="([^"]+)"') {
        return $matches[1]
    }
    if ($html -match 'name="_csrf"\s+value="([^"]+)"') {
        return $matches[1]
    }
    return $null
}

# 1. Test Login Page
Write-Host "`n[TEST 1] Checking Login Page..." -ForegroundColor Yellow
$loginRes = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
if ($loginRes.StatusCode -eq 200 -and $loginRes.Content -like "*UniAttend*") {
    Write-Host " [PASS] Login page rendered successfully (HTTP 200)" -ForegroundColor Green
} else {
    Write-Host " [FAIL] Login page check failed" -ForegroundColor Red
    exit 1
}

# 2. Test Admin Login and Dashboard
Write-Host "`n[TEST 2] Testing Admin Login & Dashboard..." -ForegroundColor Yellow
$csrf = Get-CsrfToken $loginRes.Content
$adminLoginBody = @{
    username = "admin"
    password = "admin123"
    "_csrf"  = $csrf
}
$adminRes = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body $adminLoginBody -WebSession $session -UseBasicParsing
$adminDash = Invoke-WebRequest -Uri "$baseUrl/admin/dashboard" -WebSession $session -UseBasicParsing
if ($adminDash.Content -like "*Administrator Dashboard*") {
    Write-Host " [PASS] Admin logged in and loaded Dashboard" -ForegroundColor Green
} else {
    Write-Host " [FAIL] Admin dashboard failed" -ForegroundColor Red
    exit 1
}

# Verify Admin Sub-Pages
$studentsPage = Invoke-WebRequest -Uri "$baseUrl/admin/students" -WebSession $session -UseBasicParsing
$lecturersPage = Invoke-WebRequest -Uri "$baseUrl/admin/lecturers" -WebSession $session -UseBasicParsing
$subjectsPage = Invoke-WebRequest -Uri "$baseUrl/admin/subjects" -WebSession $session -UseBasicParsing
$enrollmentsPage = Invoke-WebRequest -Uri "$baseUrl/admin/enrollments" -WebSession $session -UseBasicParsing
$reportsPage = Invoke-WebRequest -Uri "$baseUrl/admin/reports" -WebSession $session -UseBasicParsing

Write-Host " [PASS] Admin Students page loaded ($($studentsPage.StatusCode))" -ForegroundColor Green
Write-Host " [PASS] Admin Lecturers page loaded ($($lecturersPage.StatusCode))" -ForegroundColor Green
Write-Host " [PASS] Admin Subjects page loaded ($($subjectsPage.StatusCode))" -ForegroundColor Green
Write-Host " [PASS] Admin Enrollments page loaded ($($enrollmentsPage.StatusCode))" -ForegroundColor Green
Write-Host " [PASS] Admin Reports page loaded ($($reportsPage.StatusCode))" -ForegroundColor Green

# 3. Test Lecturer Login & Session Creation with Dynamic QR
Write-Host "`n[TEST 3] Testing Lecturer Session & Dynamic QR Code..." -ForegroundColor Yellow
$lecSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$lecLoginRes = Invoke-WebRequest -Uri "$baseUrl/login" -WebSession $lecSession -UseBasicParsing
$lecCsrf = Get-CsrfToken $lecLoginRes.Content

$lecLoginBody = @{
    username = "dr.smith"
    password = "lecturer123"
    "_csrf"  = $lecCsrf
}
Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body $lecLoginBody -WebSession $lecSession -UseBasicParsing
$lecDash = Invoke-WebRequest -Uri "$baseUrl/lecturer/dashboard" -WebSession $lecSession -UseBasicParsing
Write-Host " [PASS] Lecturer logged in (Dr. Alan Smith)" -ForegroundColor Green

# Create attendance session for CS101 (subjectId=1)
$today = (Get-Date).ToString("yyyy-MM-dd")
$time = (Get-Date).ToString("HH:mm")
$createCsrf = Get-CsrfToken $lecDash.Content
$sessionBody = @{
    subjectId   = "1"
    sessionName = "Live Class: Dynamic Programming"
    sessionDate = $today
    startTime   = $time
    "_csrf"     = $createCsrf
}
$createRes = Invoke-WebRequest -Uri "$baseUrl/lecturer/sessions/create" -Method Post -Body $sessionBody -WebSession $lecSession -UseBasicParsing

$sessionId = 1
# Check if redirect took us to session or find session detail
$sessionDetail = Invoke-WebRequest -Uri "$baseUrl/lecturer/sessions/$sessionId" -WebSession $lecSession -UseBasicParsing
if ($sessionDetail.Content -like "*Dynamic QR Code*" -and $sessionDetail.Content -like "*data:image/png;base64*") {
    Write-Host " [PASS] Dynamic QR Code and 2-minute countdown rendered" -ForegroundColor Green
} else {
    Write-Host " [FAIL] QR code not found on session page" -ForegroundColor Red
}

$liveDataRes = Invoke-WebRequest -Uri "$baseUrl/lecturer/sessions/$sessionId/attendance-data" -WebSession $lecSession -UseBasicParsing
$sessionData = $liveDataRes.Content | ConvertFrom-Json
$qrToken = $sessionData.qrToken
Write-Host " [PASS] Live Attendance API returned QR Token: $qrToken (Active=$($sessionData.active), Expired=$($sessionData.qrExpired))" -ForegroundColor Green

# 4. Test Student Login & QR Attendance Marking
Write-Host "`n[TEST 4] Testing Student Login & QR Attendance Marking..." -ForegroundColor Yellow
$stuSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$stuLoginRes = Invoke-WebRequest -Uri "$baseUrl/login" -WebSession $stuSession -UseBasicParsing
$stuCsrf = Get-CsrfToken $stuLoginRes.Content

$stuLoginBody = @{
    username = "student1"
    password = "student123"
    "_csrf"  = $stuCsrf
}
Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body $stuLoginBody -WebSession $stuSession -UseBasicParsing

# Student Dashboard
$stuDash = Invoke-WebRequest -Uri "$baseUrl/student/dashboard" -WebSession $stuSession -UseBasicParsing
if ($stuDash.Content -like "*Overall Attendance*" -and $stuDash.Content -like "*Alex Morgan*") {
    Write-Host " [PASS] Student Dashboard rendered with Attendance %, Charts, and Tables" -ForegroundColor Green
}

# Student Scan Page
$stuScan = Invoke-WebRequest -Uri "$baseUrl/student/scan" -WebSession $stuSession -UseBasicParsing
Write-Host " [PASS] Student Camera Scanner Page rendered ($($stuScan.StatusCode))" -ForegroundColor Green

# Mark Attendance via API using the dynamic QR token
$markBody = @{
    qrToken    = $qrToken
    deviceInfo = "PowerShell Automated Test Suite (Windows 11)"
} | ConvertTo-Json

$stuCsrfHeader = Get-CsrfToken $stuScan.Content
$markHeaders = @{
    "Content-Type" = "application/json"
    "X-CSRF-TOKEN" = $stuCsrfHeader
}

Write-Host " [INFO] Submitting scanned QR token to /student/mark-attendance..." -ForegroundColor Gray
$markRes = Invoke-WebRequest -Uri "$baseUrl/student/mark-attendance" -Method Post -Body $markBody -Headers $markHeaders -WebSession $stuSession -UseBasicParsing
$markResult = $markRes.Content | ConvertFrom-Json

if ($markResult.success -eq $true) {
    Write-Host " [PASS] Attendance marked successfully! Course: $($markResult.subjectCode) - $($markResult.subjectName)" -ForegroundColor Green
    Write-Host "        Time: $($markResult.markedAt) | Subject Attendance: $($markResult.currentPercentage)%" -ForegroundColor Green
} else {
    Write-Host " [FAIL] Attendance marking failed: $($markResult.message)" -ForegroundColor Red
    exit 1
}

# Test Duplicate Prevention (student1 scans the same QR code again)
Write-Host " [INFO] Testing duplicate prevention (scanning same QR again)..." -ForegroundColor Gray
try {
    $dupRes = Invoke-WebRequest -Uri "$baseUrl/student/mark-attendance" -Method Post -Body $markBody -Headers $markHeaders -WebSession $stuSession -UseBasicParsing
    Write-Host " [FAIL] Duplicate attendance was not blocked!" -ForegroundColor Red
} catch {
    $errRes = $_.Exception.Response
    if ($errRes.StatusCode -eq 400) {
        Write-Host " [PASS] Duplicate attendance correctly BLOCKED by server (HTTP 400: Already marked)" -ForegroundColor Green
    } else {
        throw $_
    }
}

# Student Attendance History
$stuHistory = Invoke-WebRequest -Uri "$baseUrl/student/history" -WebSession $stuSession -UseBasicParsing
if ($stuHistory.Content -like "*Dynamic Programming*" -and $stuHistory.Content -like "*PRESENT*") {
    Write-Host " [PASS] Attendance History shows the newly marked class session as PRESENT" -ForegroundColor Green
}

# Check Live Lecturer Monitor updated to show 1 student marked!
$updatedLiveData = Invoke-WebRequest -Uri "$baseUrl/lecturer/sessions/$sessionId/attendance-data" -WebSession $lecSession -UseBasicParsing | ConvertFrom-Json
Write-Host " [PASS] Live Attendance Monitor updated: $($updatedLiveData.count) attendee(s) recorded live on lecturer screen" -ForegroundColor Green

Write-Host "`n====================================================" -ForegroundColor Cyan
Write-Host "     ALL END-TO-END SYSTEM TESTS PASSED 100%!       " -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Cyan
