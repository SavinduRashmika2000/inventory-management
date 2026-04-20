package com.branchsales.dto;

import java.util.List;
import java.util.ArrayList;

public class SyncResponse {
    private int insertedCount;
    private int updatedCount;
    private int deletedCount;
    private List<SyncError> errors;

    public SyncResponse() {
        this.errors = new ArrayList<>();
    }

    public SyncResponse(int insertedCount, int updatedCount, int deletedCount, List<SyncError> errors) {
        this.insertedCount = insertedCount;
        this.updatedCount = updatedCount;
        this.deletedCount = deletedCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    // Getters and Setters
    public int getInsertedCount() { return insertedCount; }
    public void setInsertedCount(int insertedCount) { this.insertedCount = insertedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getDeletedCount() { return deletedCount; }
    public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }
    public List<SyncError> getErrors() { return errors; }
    public void setErrors(List<SyncError> errors) { this.errors = errors; }

    // Manual Builder
    public static class SyncResponseBuilder {
        private int insertedCount;
        private int updatedCount;
        private int deletedCount;
        private List<SyncError> errors;

        public SyncResponseBuilder insertedCount(int insertedCount) { this.insertedCount = insertedCount; return this; }
        public SyncResponseBuilder updatedCount(int updatedCount) { this.updatedCount = updatedCount; return this; }
        public SyncResponseBuilder deletedCount(int deletedCount) { this.deletedCount = deletedCount; return this; }
        public SyncResponseBuilder errors(List<SyncError> errors) { this.errors = errors; return this; }
        public SyncResponse build() {
            return new SyncResponse(insertedCount, updatedCount, deletedCount, errors);
        }
    }

    public static SyncResponseBuilder builder() {
        return new SyncResponseBuilder();
    }
}
