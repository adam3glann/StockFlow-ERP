package com.adam.warehouse.model;

import java.time.LocalDateTime;

public class StockCountSession {
    private int countId;
    private int startedBy;
    private String startedByName;
    private int completedBy;
    private String completedByName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String status;
    private int itemCount;
    private int openVarianceCount;

    public int getCountId() { return countId; }
    public void setCountId(int countId) { this.countId = countId; }
    public int getStartedBy() { return startedBy; }
    public void setStartedBy(int startedBy) { this.startedBy = startedBy; }
    public String getStartedByName() { return startedByName; }
    public void setStartedByName(String startedByName) { this.startedByName = startedByName; }
    public int getCompletedBy() { return completedBy; }
    public void setCompletedBy(int completedBy) { this.completedBy = completedBy; }
    public String getCompletedByName() { return completedByName; }
    public void setCompletedByName(String completedByName) { this.completedByName = completedByName; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    public int getOpenVarianceCount() { return openVarianceCount; }
    public void setOpenVarianceCount(int openVarianceCount) { this.openVarianceCount = openVarianceCount; }

    @Override
    public String toString() {
        return "#" + countId + " · " + startedAt + " · " + itemCount + " items";
    }
}
