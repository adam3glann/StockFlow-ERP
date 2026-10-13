package com.adam.warehouse.controller;

import com.adam.warehouse.model.ActivityLog;
import com.adam.warehouse.service.ActivityLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ActivityLogController {
    private final ActivityLogService service = new ActivityLogService();
    public ObservableList<ActivityLog> getLogs() { return FXCollections.observableArrayList(service.getAllLogs()); }
    public ObservableList<ActivityLog> search(String keyword) { return FXCollections.observableArrayList(service.searchLogs(keyword)); }
}
