package com.adam.warehouse.controller;

import com.adam.warehouse.model.Sale;
import com.adam.warehouse.service.SaleService;
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
