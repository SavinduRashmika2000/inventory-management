package com.branchsales.service;

import com.branchsales.dto.BatchSyncResult;
import com.branchsales.dto.SyncError;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyncBatchProcessor {
    private static final Logger log = LoggerFactory.getLogger(SyncBatchProcessor.class);

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchSyncResult executeUpsertBatch(String tableName, List<Map<String, Object>> records, String pkColumn, int startIndex) {
        BatchSyncResult result = new BatchSyncResult();
        
        Map<Set<String>, List<Map<String, Object>>> groups = records.stream()
                .collect(Collectors.groupingBy(Map::keySet));

        for (Map.Entry<Set<String>, List<Map<String, Object>>> entry : groups.entrySet()) {
            Set<String> columns = entry.getKey();
            List<Map<String, Object>> groupRecords = entry.getValue();
            
            String sql = buildUpsertSql(tableName, columns, pkColumn);
            List<Object[]> batchArgs = groupRecords.stream()
                    .map(r -> columns.stream().map(r::get).toArray())
                    .collect(Collectors.toList());

            try {
                int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
                parseBatchResults(updateCounts, result);
            } catch (DataAccessException e) {
                log.warn("Batch update failed for table {}, falling back to row-by-row: {}", tableName, e.getMessage());
                processRowByRow(sql, groupRecords, startIndex, result);
            }
        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchSyncResult executeDeleteBatch(String tableName, String pkColumn, List<Object> ids, int startIndex) {
        String sql = "DELETE FROM " + tableName + " WHERE " + pkColumn + " = ?";
        BatchSyncResult result = new BatchSyncResult();
        
        try {
            List<Object[]> batchArgs = ids.stream().map(id -> new Object[]{id}).collect(Collectors.toList());
            int[] deleteCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
            for (int count : deleteCounts) {
                result.setDeletedCount(result.getDeletedCount() + (count > 0 ? 1 : 0));
            }
        } catch (DataAccessException e) {
            log.warn("Delete batch failed for table {}, falling back to row-by-row: {}", tableName, e.getMessage());
            for (int i = 0; i < ids.size(); i++) {
                try {
                    int count = jdbcTemplate.update(sql, ids.get(i));
                    result.setDeletedCount(result.getDeletedCount() + (count > 0 ? 1 : 0));
                } catch (Exception ex) {
                    result.getErrors().add(new SyncError(startIndex + i, "Delete failed: " + ex.getMessage()));
                }
            }
        }
        return result;
    }

    private void processRowByRow(String sql, List<Map<String, Object>> records, int globalStartIndex, BatchSyncResult result) {
        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            try {
                Object[] args = record.values().toArray();
                int count = jdbcTemplate.update(sql, args);
                // MySQL: 1=inserted, 2=updated, 0=no change (handle as update)
                if (count == 1) result.setInsertedCount(result.getInsertedCount() + 1);
                else result.setUpdatedCount(result.getUpdatedCount() + 1);
            } catch (Exception e) {
                result.getErrors().add(new SyncError(globalStartIndex + i, "Row processing failed: " + e.getMessage()));
            }
        }
    }

    private void parseBatchResults(int[] updateCounts, BatchSyncResult result) {
        for (int count : updateCounts) {
            if (count == 1) result.setInsertedCount(result.getInsertedCount() + 1);
            else if (count == 2 || count == 0) result.setUpdatedCount(result.getUpdatedCount() + 1);
        }
    }

    private String buildUpsertSql(String tableName, Set<String> columns, String pkColumn) {
        String cols = String.join(", ", columns);
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        
        String updateClause = columns.stream()
                .filter(c -> !c.equalsIgnoreCase(pkColumn))
                .map(c -> c + " = VALUES(" + c + ")")
                .collect(Collectors.joining(", "));

        return String.format("INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s",
                tableName, cols, placeholders, updateClause);
    }
}
