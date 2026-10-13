package com.adam.warehouse.controller;

import com.adam.warehouse.model.Purchase;
import com.adam.warehouse.service.PurchaseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController() {

        purchaseService = new PurchaseService();

    }

    public void savePurchase(Purchase purchase) {

        purchaseService.savePurchase(purchase);

    }

    public ObservableList<Purchase> getPurchases() {
        return FXCollections.observableArrayList(purchaseService.getAllPurchases());
    }

    public Purchase getPurchaseById(int purchaseId) { return purchaseService.getPurchaseById(purchaseId); }

    public java.util.List<Purchase> searchPurchases(String keyword) {
        return purchaseService.searchPurchases(keyword);
    }

}
