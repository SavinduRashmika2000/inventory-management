package com.branchsales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import com.branchsales.dto.BatchSyncResult;
import com.branchsales.dto.SyncError;
import com.branchsales.dto.SyncResponse;
import com.branchsales.dto.SyncStatusResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class SyncBatchProcessor {
    private static final Logger log = LoggerFactory.getLogger(SyncBatchProcessor.class);

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchSyncResult executeUpsertBatch(String tableName, List<Map<String, Object>> batch, String pkColumn, int startIndex) {
        BatchSyncResult result = new BatchSyncResult();
        if (batch.isEmpty()) return result;

        // Group records by their set of keys (Partial Update partitions)
        Map<Set<String>, List<Map<String, Object>>> partitions = new LinkedHashMap<>();
        for (Map<String, Object> record : batch) {
            partitions.computeIfAbsent(new HashSet<>(record.keySet()), k -> new ArrayList<>()).add(record);
        }

        int currentBatchOffset = 0;
        for (Map.Entry<Set<String>, List<Map<String, Object>>> entry : partitions.entrySet()) {
            Set<String> keys = entry.getKey();
            List<Map<String, Object>> subBatch = entry.getValue();
            
            try {
                result.add(processSubBatch(tableName, subBatch, keys, pkColumn));
            } catch (Exception e) {
                log.warn("Batch failed for table {}, falling back to row-by-row: {}", tableName, e.getMessage());
                result.add(processRowByRow(tableName, subBatch, keys, pkColumn, startIndex + currentBatchOffset));
            }
            currentBatchOffset += subBatch.size();
        }

        return result;
    }

    private BatchSyncResult processSubBatch(String tableName, List<Map<String, Object>> subBatch, Set<String> keys, String pkColumn) {
        List<String> columns = new ArrayList<>(keys);
        String sql = buildUpsertSql(tableName, columns, pkColumn);

        int[][] results = jdbcTemplate.batchUpdate(sql, subBatch, subBatch.size(), (ps, record) -> {
            for (int i = 0; i < columns.size(); i++) {
                ps.setObject(i + 1, record.get(columns.get(i)));
            }
        });

        return parseBatchResults(results, false);
    }

    private BatchSyncResult processRowByRow(String tableName, List<Map<String, Object>> subBatch, Set<String> keys, String pkColumn, int startIndex) {
        BatchSyncResult result = new BatchSyncResult();
        List<String> columns = new ArrayList<>(keys);
        String sql = buildUpsertSql(tableName, columns, pkColumn);

        for (int i = 0; i < subBatch.size(); i++) {
            Map<String, Object> record = subBatch.get(i);
            try {
                int rowResult = jdbcTemplate.update(sql, columns.stream().map(record::get).toArray());
                result.add(parseSingleResult(rowResult, false));
            } catch (DataAccessException e) {
                result.getErrors().add(new SyncError(startIndex + i, e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage()));
            }
        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchSyncResult executeDeleteBatch(String tableName, String pkColumn, List<Object> ids, int startIndex) {
        BatchSyncResult result = new BatchSyncResult();
        if (ids.isEmpty()) return result;

        String sql = String.format("DELETE FROM %s WHERE %s = ?", tableName, pkColumn);

        try {
            int[][] results = jdbcTemplate.batchUpdate(sql, ids, ids.size(), (ps, id) -> {
                ps.setObject(1, id);
            });
            result.add(parseBatchResults(results, true));
        } catch (Exception e) {
            log.warn("Delete batch failed for table {}, falling back to row-by-row", tableName);
            for (int i = 0; i < ids.size(); i++) {
                try {
                    int rows = jdbcTemplate.update(sql, ids.get(i));
                    result.setDeletedCount(result.getDeletedCount() + rows);
                } catch (DataAccessException ex) {
                    result.getErrors().add(new SyncError(startIndex + i, ex.getMessage()));
                }
            }
        }

        return result;
    }

    private String buildUpsertSql(String tableName, List<String> columns, String pkColumn) {
        String colNames = String.join(", ", columns);
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String updatePart = columns.stream()
                .filter(c -> !c.equalsIgnoreCase(pkColumn)) // Exclude PK from update clause
                .map(c -> c + " = VALUES(" + c + ")")
                .collect(Collectors.joining(", "));

        return String.format(
                "INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s",
                tableName, colNames, placeholders, updatePart
        );
    }

    private BatchSyncResult parseBatchResults(int[][] results, boolean isDelete) {
        BatchSyncResult batchResult = new BatchSyncResult();
        for (int[] group : results) {
            for (int r : group) {
                batchResult.add(parseSingleResult(r, isDelete));
            }
        }
        return batchResult;
    }

    private BatchSyncResult parseSingleResult(int r, boolean isDelete) {
        BatchSyncResult result = new BatchSyncResult();
        if (isDelete) {
            if (r > 0 || r == -2) result.setDeletedCount(1);
        } else {
            if (r == 1) {
                result.setInsertedCount(1);
            } else if (r == 2 || r == 0 || r == -2) {
                result.setUpdatedCount(1);
            }
        }
        return result;
    }
}
