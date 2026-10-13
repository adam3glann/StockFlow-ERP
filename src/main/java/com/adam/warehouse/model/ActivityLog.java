package com.adam.warehouse.model;

import java.time.LocalDateTime;

public class ActivityLog {

    private int logId;

    private int employeeId;

    private String employeeName;

    private String module;

    private String action;

    private String description;

    private LocalDateTime timestamp;

    public ActivityLog() {
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLog(int employeeId,
                       String employeeName,
                       String module,
                       String action,
                       String description) {

        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.module = module;
        this.action = action;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName == null ? "" : employeeName.trim();
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module == null ? "" : module.trim().toUpperCase();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action == null ? "" : action.trim().toUpperCase();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedAction() {
        return module + " - " + action;
    }

    @Override
    public String toString() {

        return "[" + timestamp + "] "
                + employeeName
                + " | "
                + module
                + " | "
                + action;

    }

}