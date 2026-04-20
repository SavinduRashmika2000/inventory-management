package com.branchsales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncError {
    private int index;
    private String message;

    public SyncError() {}
    public SyncError(int index, String message) {
        this.index = index;
        this.message = message;
    }
    // Getters and Setters
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
