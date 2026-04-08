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
                .insertedCount(totalInsertedOrUpdated) // MySQL returns 1 for insert and 2 for update in some cases, so this is an aggregate
                .updatedCount(0) // We can't easily distinguish without more parsing
                .build();
    }

    private List<String> getTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch metadata for table: " + tableName, e);
        }
        return columns;
    }

    private List<String> validateRecords(List<Map<String, Object>> records, List<String> schemaColumns) {
        List<String> errors = new ArrayList<>();
        Set<String> schemaSet = new HashSet<>(schemaColumns);

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            Set<String> recordKeys = record.keySet();

            if (!schemaSet.equals(recordKeys)) {
                List<String> missing = schemaColumns.stream().filter(c -> !recordKeys.contains(c)).collect(Collectors.toList());
                List<String> extra = recordKeys.stream().filter(k -> !schemaSet.contains(k)).collect(Collectors.toList());
                
                String error = "Record index " + i + " has mismatching fields. ";
                if (!missing.isEmpty()) error += "Missing: " + missing + ". ";
                if (!extra.isEmpty()) error += "Extra: " + extra + ". ";
                errors.add(error);
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

        try (Connection conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(catalog, null, tableName, null)) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Table does not exist in the current database: " + tableName);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error validating table existence: " + tableName, e);
        }
    }
}
