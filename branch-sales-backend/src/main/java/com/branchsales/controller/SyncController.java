package com.branchsales.controller;

import com.branchsales.dto.SyncError;
import com.branchsales.dto.SyncResponse;
import com.branchsales.dto.SyncStatusResponse;
import com.branchsales.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

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
                    .errors(List.of(new SyncError(-1, e.getMessage())))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                    .errors(List.of(new SyncError(-1, "Internal server error: " + e.getMessage())))
                    .build());
        }
    }

    @DeleteMapping("/{tableName}")
    public ResponseEntity<SyncResponse> deleteRecords(
            @PathVariable String tableName,
            @RequestBody List<Object> ids
    ) {
        try {
            SyncResponse response = syncService.deleteRecords(tableName, ids);
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(SyncResponse.builder()
                    .errors(List.of(new SyncError(-1, e.getMessage())))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(SyncResponse.builder()
                    .errors(List.of(new SyncError(-1, "Internal server error: " + e.getMessage())))
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
