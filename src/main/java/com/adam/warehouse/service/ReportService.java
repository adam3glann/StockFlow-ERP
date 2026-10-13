package com.adam.warehouse.service;

import com.adam.warehouse.dao.ReportDAO;
import com.adam.warehouse.model.ReportSummary;
import com.adam.warehouse.model.TopSellingProduct;
import com.adam.warehouse.util.PermissionManager;
import java.time.LocalDate;
import java.util.List;

/** Business entry point for aggregate operational reports. */
public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();

    public ReportSummary getSummary() {
        PermissionManager.require(PermissionManager.canViewReports(), "view reports");
        return reportDAO.getSummary();
    }

    public ReportSummary getTodaySummary() {
        PermissionManager.require(PermissionManager.canViewReports(), "view reports");
        return reportDAO.getTodaySummary();
    }

    public ReportSummary getDateRangeSummary(LocalDate start, LocalDate end) {
        PermissionManager.require(PermissionManager.canViewReports(), "view reports");
        return reportDAO.getDateRangeSummary(start, end);
    }

    public List<TopSellingProduct> getTopSellingProducts(LocalDate start, LocalDate end, int limit) {
        PermissionManager.require(PermissionManager.canViewReports(), "view reports");
        return reportDAO.getTopSellingProducts(start, end, limit);
    }
}
