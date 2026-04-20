package com.branchsales.dto;

import java.time.LocalDateTime;

public class SyncStatusResponse {
    private String tableName;
    private LocalDateTime lastSyncTimestamp;
    private int totalRecords;

    public SyncStatusResponse() {}

    private SyncStatusResponse(Builder builder) {
        this.tableName = builder.tableName;
        this.lastSyncTimestamp = builder.lastSyncTimestamp;
        this.totalRecords = builder.totalRecords;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public LocalDateTime getLastSyncTimestamp() { return lastSyncTimestamp; }
    public void setLastSyncTimestamp(LocalDateTime lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public static class Builder {
        private String tableName;
        private LocalDateTime lastSyncTimestamp;
        private int totalRecords;

        public Builder tableName(String val) { tableName = val; return this; }
        public Builder lastSyncTimestamp(LocalDateTime val) { lastSyncTimestamp = val; return this; }
        public Builder totalRecords(int val) { totalRecords = val; return this; }
        public SyncStatusResponse build() { return new SyncStatusResponse(this); }
    }
}
