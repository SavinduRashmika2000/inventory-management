package com.branchsales.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchSyncResult {
    private int insertedCount;
    private int updatedCount;
    private int deletedCount;
    private List<SyncError> errors = new ArrayList<>();

    public BatchSyncResult() {}

    public BatchSyncResult(int insertedCount, int updatedCount, int deletedCount, List<SyncError> errors) {
        this.insertedCount = insertedCount;
        this.updatedCount = updatedCount;
        this.deletedCount = deletedCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public int getInsertedCount() { return insertedCount; }
    public void setInsertedCount(int insertedCount) { this.insertedCount = insertedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getDeletedCount() { return deletedCount; }
    public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }
    public List<SyncError> getErrors() { return errors; }
    public void setErrors(List<SyncError> errors) { this.errors = errors; }

    public void add(BatchSyncResult other) {
        this.insertedCount += other.insertedCount;
        this.updatedCount += other.updatedCount;
        this.deletedCount += other.deletedCount;
        this.errors.addAll(other.errors);
    }

    public static class BatchSyncResultBuilder {
        private int insertedCount;
        private int updatedCount;
        private int deletedCount;
        private List<SyncError> errors = new ArrayList<>();

        public BatchSyncResultBuilder insertedCount(int insertedCount) { this.insertedCount = insertedCount; return this; }
        public BatchSyncResultBuilder updatedCount(int updatedCount) { this.updatedCount = updatedCount; return this; }
        public BatchSyncResultBuilder deletedCount(int deletedCount) { this.deletedCount = deletedCount; return this; }
        public BatchSyncResultBuilder errors(List<SyncError> errors) { this.errors = errors; return this; }
        public BatchSyncResult build() {
            return new BatchSyncResult(insertedCount, updatedCount, deletedCount, errors);
        }
    }

    public static BatchSyncResultBuilder builder() {
        return new BatchSyncResultBuilder();
    }
}
