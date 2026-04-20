package com.branchsales.dto;

import java.util.List;

public class SyncResponse {
    private int insertedCount;
    private int updatedCount;
    private int deletedCount;
    private long durationMs;
    private List<SyncError> errors;

    public SyncResponse() {}

    private SyncResponse(Builder builder) {
        this.insertedCount = builder.insertedCount;
        this.updatedCount = builder.updatedCount;
        this.deletedCount = builder.deletedCount;
        this.durationMs = builder.durationMs;
        this.errors = builder.errors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getInsertedCount() { return insertedCount; }
    public void setInsertedCount(int insertedCount) { this.insertedCount = insertedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getDeletedCount() { return deletedCount; }
    public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public List<SyncError> getErrors() { return errors; }
    public void setErrors(List<SyncError> errors) { this.errors = errors; }

    public static class Builder {
        private int insertedCount;
        private int updatedCount;
        private int deletedCount;
        private long durationMs;
        private List<SyncError> errors;

        public Builder insertedCount(int val) { insertedCount = val; return this; }
        public Builder updatedCount(int val) { updatedCount = val; return this; }
        public Builder deletedCount(int val) { deletedCount = val; return this; }
        public Builder durationMs(long val) { durationMs = val; return this; }
        public Builder errors(List<SyncError> val) { errors = val; return this; }
        public SyncResponse build() { return new SyncResponse(this); }
    }
}
