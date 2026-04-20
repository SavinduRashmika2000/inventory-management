
function Run-Test {
    param($Name, $Method, $Url, $Body)
    Write-Host "`n--- Testing: $Name ---"
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            ContentType = "application/json"
        }
        if ($Body) { $params.Body = $Body }
        
        $startTime = Get-Date
        $res = Invoke-RestMethod @params
        $endTime = Get-Date
        $duration = ($endTime - $startTime).TotalMilliseconds
        
        Write-Host "Status: SUCCESS ($duration ms)"
        Write-Host "Response: $($res | ConvertTo-Json -Compress)"
        return $res
    } catch {
        Write-Host "Status: FAILED"
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $err = $reader.ReadToEnd()
            Write-Host "Error Body: $err"
        } else {
            Write-Host "Error: $($_.Exception.Message)"
        }
        return $null
    }
}

$baseUrl = "http://localhost:8080/api/sync"

# 1. Supplier Tests
Run-Test "Initial Status (Supplier)" "Get" "$baseUrl/status/supplier"
$supplierJson = '[{"idsupplier": 88888, "name": "Global Parts Inc", "tel": "0112223334", "status": 1, "cloud": 1}]'
Run-Test "Upsert Insert (Supplier)" "Post" "$baseUrl/supplier" $supplierJson
Run-Test "Upsert Update (Supplier)" "Post" "$baseUrl/supplier" '[{"idsupplier": 88888, "tel": "0999999999"}]'
Run-Test "Delete (Supplier)" "Delete" "$baseUrl/supplier" "[88888]"

# 2. Main Item Tests
Run-Test "Initial Status (Main Item)" "Get" "$baseUrl/status/main_item"
$mainItemJson = '[{"idmain_item": 77777, "name": "Sync Test Product", "code": "SYNC-001", "status": 1, "Main_category_idMain_category": 1, "cloud": 1}]'
Run-Test "Upsert Insert (Main Item)" "Post" "$baseUrl/main_item" $mainItemJson
Run-Test "Delete (Main Item)" "Delete" "$baseUrl/main_item" "[77777]"
