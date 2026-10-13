package com.adam.warehouse.controller;

import com.adam.warehouse.model.Sale;
import com.adam.warehouse.service.SaleService;
import com.adam.warehouse.enums.PaymentMethod;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SaleController {

    private final SaleService saleService;

    public SaleController() {
        saleService = new SaleService();
    }

    public void saveSale(Sale sale) {
        saleService.saveSale(sale);
    }

    public void recordPayment(int saleId, double amount, PaymentMethod method, double tendered, String reference) {
        saleService.recordPayment(saleId, amount, method, tendered, reference);
    }
    public ObservableList<Sale> getSales() {

        return FXCollections.observableArrayList(
                saleService.getAllSales()
        );

    }

    public Sale getSaleById(int saleId) { return saleService.getSaleById(saleId); }

    public ObservableList<Sale> searchSales(String keyword) {
        return FXCollections.observableArrayList(
                saleService.searchSales(keyword)
        );
    }

    public void returnSaleItem(int saleId, int saleItemId, int returned, String text) {
    }
}
