
# 1. Test Status (Expected 404 for new table)
Write-Host "Testing Status (Initial)..."
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/sync/status/customer" -Method Get
} catch {
    Write-Host "Caught expected 404: $($_.Exception.Message)"
}

# 2. Test Upsert (Insert)
Write-Host "Testing Upsert (Insert)..."
$customerData = '[{"idcustomer": 99999, "name": "Sync Test", "address": "123 Sync St", "tel": "0771234567", "status": 1, "des": "TEST", "cloud": 1}]'
try {
    $res1 = Invoke-RestMethod -Uri "http://localhost:8080/api/sync/customer" -Method Post -Body $customerData -ContentType "application/json"
    Write-Host "Insert Response: $($res1 | ConvertTo-Json -Compress)"
} catch {
    $errorBody = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($errorBody)
    $responseString = $reader.ReadToEnd()
    Write-Host "Insert failed: $responseString"
}

# 3. Test Status (Now expected 200)
Write-Host "Testing Status (After Insert)..."
$status = Invoke-RestMethod -Uri "http://localhost:8080/api/sync/status/customer" -Method Get
Write-Host "Status Response: $($status | ConvertTo-Json -Compress)"

# 4. Test Upsert (Update)
Write-Host "Testing Upsert (Update)..."
$customerDataUpdate = '[{"idcustomer": 99999, "name": "Sync Test Updated"}]'
$res2 = Invoke-RestMethod -Uri "http://localhost:8080/api/sync/customer" -Method Post -Body $customerDataUpdate -ContentType "application/json"
Write-Host "Update Response: $($res2 | ConvertTo-Json -Compress)"

# 5. Test Delete
Write-Host "Testing Delete..."
$deleteData = '[99999]'
$res3 = Invoke-RestMethod -Uri "http://localhost:8080/api/sync/customer" -Method Delete -Body $deleteData -ContentType "application/json"
Write-Host "Delete Response: $($res3 | ConvertTo-Json -Compress)"

# 6. Final Status Check
Write-Host "Final Status Check..."
$finalStatus = Invoke-RestMethod -Uri "http://localhost:8080/api/sync/status/customer" -Method Get
Write-Host "Final Status: $($finalStatus | ConvertTo-Json -Compress)"
