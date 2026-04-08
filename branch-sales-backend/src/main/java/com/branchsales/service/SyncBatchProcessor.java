package com.branchsales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyncBatchProcessor {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int executeUpsertBatch(String tableName, List<Map<String, Object>> batch, List<String> columns) {
        String colNames = String.join(", ", columns);
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String updatePart = columns.stream()
                .map(c -> c + " = VALUES(" + c + ")")
                .collect(Collectors.joining(", "));

        String sql = String.format(
                "INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s",
                tableName, colNames, placeholders, updatePart
        );

        int[][] results = jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, record) -> {
            for (int i = 0; i < columns.size(); i++) {
                ps.setObject(i + 1, record.get(columns.get(i)));
            }
        });

        int affected = 0;
        for (int[] row : results) {
            for (int r : row) {
                if (r > 0 || r == -2) affected++; // -2 means Success but unknown affected count for some JDBC drivers
            }
        }
        return affected;
    }
}
