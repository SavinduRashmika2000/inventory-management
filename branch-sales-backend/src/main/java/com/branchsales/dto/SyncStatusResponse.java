package com.branchsales.dto;

import java.time.LocalDateTime;

public class SyncStatusResponse {
    private String tableName;
    private LocalDateTime lastSyncTimestamp;
    private int totalRecords;

    public SyncStatusResponse() {}
    public SyncStatusResponse(String tableName, LocalDateTime lastSyncTimestamp, int totalRecords) {
        this.tableName = tableName;
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.totalRecords = totalRecords;
    }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public LocalDateTime getLastSyncTimestamp() { return lastSyncTimestamp; }
    public void setLastSyncTimestamp(LocalDateTime lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public static class SyncStatusResponseBuilder {
        private String tableName;
        private LocalDateTime lastSyncTimestamp;
        private int totalRecords;

        public SyncStatusResponseBuilder tableName(String tableName) { this.tableName = tableName; return this; }
        public SyncStatusResponseBuilder lastSyncTimestamp(LocalDateTime lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; return this; }
        public SyncStatusResponseBuilder totalRecords(int totalRecords) { this.totalRecords = totalRecords; return this; }
        public SyncStatusResponse build() {
            return new SyncStatusResponse(tableName, lastSyncTimestamp, totalRecords);
        }
    }

    public static SyncStatusResponseBuilder builder() {
        return new SyncStatusResponseBuilder();
    }
}
