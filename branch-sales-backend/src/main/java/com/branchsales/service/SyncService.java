package com.branchsales.service;

import com.branchsales.dto.BatchSyncResult;
import com.branchsales.dto.SyncError;
import com.branchsales.dto.SyncResponse;
import com.branchsales.dto.SyncStatusResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyncService {
    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final SyncBatchProcessor batchProcessor;

    @PostConstruct
    public void init() {
        String sql = "CREATE TABLE IF NOT EXISTS sync_status (" +
                "table_name VARCHAR(255) PRIMARY KEY, " +
                "last_sync_timestamp DATETIME, " +
                "total_records INT" +
                ")";
        jdbcTemplate.execute(sql);
    }

    public SyncStatusResponse getSyncStatus(String tableName) {
        validateTable(tableName);

        String sql = "SELECT * FROM sync_status WHERE table_name = ?";
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return SyncStatusResponse.builder()
                        .tableName(rs.getString("table_name"))
                        .lastSyncTimestamp(rs.getTimestamp("last_sync_timestamp").toLocalDateTime())
                        .totalRecords(rs.getInt("total_records"))
                        .build();
            }
            return null;
        }, tableName);
    }

    public SyncResponse syncTable(String tableName, List<Map<String, Object>> records) {
        validateTable(tableName);

        if (records == null || records.isEmpty()) {
            return SyncResponse.builder().errors(Collections.singletonList(new SyncError(-1, "No records provided"))).build();
        }

        String pkColumn = getPrimaryKeyColumn(tableName);
        if (pkColumn == null) {
            return SyncResponse.builder().errors(Collections.singletonList(new SyncError(-1, "Could not identify primary key for table"))).build();
        }

        Set<String> schemaColumns = new HashSet<>(getTableColumns(tableName));
        
        List<SyncError> businessErrors = new ArrayList<>();
        List<Map<String, Object>> validRecords = new ArrayList<>();
        
        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            String error = validateRecord(record, schemaColumns, pkColumn);
            if (error != null) {
                businessErrors.add(new SyncError(i, error));
            } else {
                validRecords.add(record);
            }
        }

        BatchSyncResult finalResult = new BatchSyncResult();
        finalResult.setErrors(businessErrors);

        if (!validRecords.isEmpty()) {
            int batchSize = 500;
            for (int i = 0; i < validRecords.size(); i += batchSize) {
                int end = Math.min(i + batchSize, validRecords.size());
                List<Map<String, Object>> batch = validRecords.subList(i, end);
                finalResult.add(batchProcessor.executeUpsertBatch(tableName, batch, pkColumn, i));
            }
        }

        updateSyncStatus(tableName, validRecords.size());

        return SyncResponse.builder()
                .insertedCount(finalResult.getInsertedCount())
                .updatedCount(finalResult.getUpdatedCount())
                .errors(finalResult.getErrors())
                .build();
    }

    public SyncResponse deleteRecords(String tableName, List<Object> ids) {
        validateTable(tableName);

        if (ids == null || ids.isEmpty()) {
            return SyncResponse.builder().errors(Collections.singletonList(new SyncError(-1, "No IDs provided"))).build();
        }

        String pkColumn = getPrimaryKeyColumn(tableName);
        if (pkColumn == null) {
             return SyncResponse.builder().errors(Collections.singletonList(new SyncError(-1, "Could not identify primary key"))).build();
        }

        BatchSyncResult finalResult = new BatchSyncResult();
        int batchSize = 500;
        for (int i = 0; i < ids.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ids.size());
            finalResult.add(batchProcessor.executeDeleteBatch(tableName, pkColumn, ids.subList(i, end), i));
        }

        return SyncResponse.builder()
                .deletedCount(finalResult.getDeletedCount())
                .errors(finalResult.getErrors())
                .build();
    }

    private String getPrimaryKeyColumn(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
                if (rs.next()) {
                    return rs.getString("COLUMN_NAME");
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching PK for table {}: {}", tableName, e.getMessage());
        }
        return null;
    }

    private List<String> getTableColumns(String tableName) {
        String sql = "DESCRIBE " + tableName;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("Field"));
    }

    private String validateRecord(Map<String, Object> record, Set<String> schemaColumns, String pkColumn) {
        if (!record.containsKey(pkColumn)) {
            return "Missing mandatory primary key: " + pkColumn;
        }
        
        List<String> unknown = record.keySet().stream()
                .filter(k -> !schemaColumns.contains(k))
                .collect(Collectors.toList());
        
        if (!unknown.isEmpty()) {
            return "Unknown columns: " + unknown;
        }
        
        return null;
    }

    private void updateSyncStatus(String tableName, int recordCount) {
        String sql = "INSERT INTO sync_status (table_name, last_sync_timestamp, total_records) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE last_sync_timestamp = VALUES(last_sync_timestamp), total_records = VALUES(total_records)";
        jdbcTemplate.update(sql, tableName, LocalDateTime.now(), recordCount);
    }

    private void validateTable(String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name format");
        }

        String sql = "SHOW TABLES LIKE ?";
        List<String> tables = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), tableName);
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("Table does not exist: " + tableName);
        }
    }
}
