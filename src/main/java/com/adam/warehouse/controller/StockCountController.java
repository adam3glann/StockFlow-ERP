package com.adam.warehouse.controller;

import com.adam.warehouse.model.StockCountItem;
import com.adam.warehouse.model.StockCountSession;
import com.adam.warehouse.service.StockCountService;

import java.util.List;

public class StockCountController {
    private final StockCountService service = new StockCountService();

    public StockCountSession startCount() { return service.startCount(); }
    public List<StockCountSession> getOpenSessions() { return service.getOpenSessions(); }
    public List<StockCountSession> getClosedSessions() { return service.getClosedSessions(); }
    public void cancelCount(int countId) { service.cancelCount(countId); }
    public List<StockCountItem> getCountItems(int countId) { return service.getCountItems(countId); }
    public List<StockCountItem> getOpenVariances() { return service.getOpenVariances(); }
    public void completeCount(int countId, List<StockCountItem> items) { service.completeCount(countId, items); }
    public void resolveVariance(int countItemId, String notes) { service.resolveVariance(countItemId, notes); }
}
