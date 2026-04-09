package com.branchsales.service;

import com.branchsales.dto.SyncResponse;
import com.branchsales.dto.SyncStatusResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            return SyncResponse.builder().errors(Collections.singletonList("No records provided")).build();
        }

        List<String> columns = getTableColumns(tableName);
        List<String> errors = validateRecords(records, columns);

        if (!errors.isEmpty()) {
            return SyncResponse.builder().errors(errors).build();
        }

        // Batch processing in chunks of 500
        int batchSize = 500;
        int totalInsertedOrUpdated = 0;

        for (int i = 0; i < records.size(); i += batchSize) {
            int end = Math.min(i + batchSize, records.size());
            List<Map<String, Object>> batch = records.subList(i, end);
            totalInsertedOrUpdated += batchProcessor.executeUpsertBatch(tableName, batch, columns);
        }

        updateSyncStatus(tableName, records.size());

        return SyncResponse.builder()
                .insertedCount(totalInsertedOrUpdated)
                .updatedCount(0)
                .build();
    }

    public SyncResponse deleteRecords(String tableName, List<Object> ids) {
        validateTable(tableName);

        if (ids == null || ids.isEmpty()) {
            return SyncResponse.builder().errors(Collections.singletonList("No IDs provided")).build();
        }

        String pkColumn = getPrimaryKeyColumn(tableName);
        if (pkColumn == null) {
            return SyncResponse.builder().errors(Collections.singletonList("Could not identify primary key for table: " + tableName)).build();
        }

        int batchSize = 500;
        int totalDeleted = 0;

        for (int i = 0; i < ids.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ids.size());
            List<Object> batch = ids.subList(i, end);
            totalDeleted += batchProcessor.executeDeleteBatch(tableName, pkColumn, batch);
        }

        return SyncResponse.builder()
                .updatedCount(totalDeleted) // Using updatedCount to represent deletions
                .build();
    }

    private String getPrimaryKeyColumn(String tableName) {
        String sql = "SHOW KEYS FROM " + tableName + " WHERE Key_name = 'PRIMARY'";
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return rs.getString("Column_name");
            }
            return null;
        });
    }

    private List<String> getTableColumns(String tableName) {
        String sql = "DESCRIBE " + tableName;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("Field"));
    }

    private List<String> validateRecords(List<Map<String, Object>> records, List<String> schemaColumns) {
        List<String> errors = new ArrayList<>();
        Set<String> schemaSet = new HashSet<>(schemaColumns);

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            // Only reject records that have columns NOT in the schema (extra/unknown columns).
            // Missing columns are perfectly fine — partial updates are supported.
            List<String> extra = record.keySet().stream()
                    .filter(k -> !schemaSet.contains(k))
                    .collect(Collectors.toList());
            if (!extra.isEmpty()) {
                errors.add("Record index " + i + " contains unknown columns not in table schema: " + extra);
            }
        }
        return errors;
    }

    private void updateSyncStatus(String tableName, int recordCount) {
        String sql = "INSERT INTO sync_status (table_name, last_sync_timestamp, total_records) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE last_sync_timestamp = VALUES(last_sync_timestamp), total_records = VALUES(total_records)";
        jdbcTemplate.update(sql, tableName, LocalDateTime.now(), recordCount);
    }

    private void validateTable(String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name format: " + tableName);
        }

        // Use a direct query for validation (more reliable than Metadata API in some environments)
        String sql = "SHOW TABLES LIKE ?";
        List<String> tables = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1), tableName);
        
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("Table does not exist in the database: " + tableName);
        }
    }
}
