package com.adam.warehouse.controller;

import com.adam.warehouse.model.PurchaseReturn;
import com.adam.warehouse.model.ReturnablePurchaseItem;
import com.adam.warehouse.service.PurchaseReturnService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    public PurchaseReturnController() {
        purchaseReturnService = new PurchaseReturnService();
    }

    public void saveReturn(PurchaseReturn purchaseReturn) {
        purchaseReturnService.saveReturn(purchaseReturn);
    }

    public ObservableList<PurchaseReturn> getReturns() {
        return FXCollections.observableArrayList(purchaseReturnService.getAllReturns());
    }

    public ObservableList<PurchaseReturn> getReturnsForPurchase(int purchaseId) {
        return FXCollections.observableArrayList(purchaseReturnService.getReturnsForPurchase(purchaseId));
    }

    public PurchaseReturn getReturnById(int purchaseReturnId) {
        return purchaseReturnService.getReturnById(purchaseReturnId);
    }

    public List<ReturnablePurchaseItem> getReturnableItems(int purchaseId) {
        return purchaseReturnService.getReturnableItems(purchaseId);
    }
}
