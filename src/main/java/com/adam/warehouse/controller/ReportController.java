package com.adam.warehouse.controller;

import com.adam.warehouse.model.ReportSummary;
import com.adam.warehouse.model.TopSellingProduct;
import com.adam.warehouse.service.ReportService;
import java.time.LocalDate;
import java.util.List;

public class ReportController {

    private final ReportService reportService = new ReportService();

    public ReportSummary getSummary() {
        return reportService.getSummary();
    }

    public ReportSummary getTodaySummary() {
        return reportService.getTodaySummary();
    }
    public ReportSummary getDateRangeSummary(LocalDate start, LocalDate end) { return reportService.getDateRangeSummary(start, end); }

    public List<TopSellingProduct> getTopSellingProducts(LocalDate start, LocalDate end, int limit) {
        return reportService.getTopSellingProducts(start, end, limit);
    }
}
