package com.branchsales.controller;

import com.branchsales.dto.SyncResponse;
import com.branchsales.dto.SyncStatusResponse;
import com.branchsales.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/{tableName}")
    public ResponseEntity<SyncResponse> syncTable(
            @PathVariable String tableName,
            @RequestBody List<Map<String, Object>> records
    ) {
        try {
            SyncResponse response = syncService.syncTable(tableName, records);
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(SyncResponse.builder()
                    .errors(List.of(e.getMessage()))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                    .errors(List.of("Internal server error: " + e.getMessage()))
                    .build());
        }
    }

    @GetMapping("/status/{tableName}")
    public ResponseEntity<SyncStatusResponse> getSyncStatus(@PathVariable String tableName) {
        try {
            SyncStatusResponse status = syncService.getSyncStatus(tableName);
            if (status == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
