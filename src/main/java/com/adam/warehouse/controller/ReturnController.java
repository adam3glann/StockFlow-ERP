package com.adam.warehouse.controller;

import com.adam.warehouse.model.Return;
import com.adam.warehouse.model.ReturnableItem;
import com.adam.warehouse.service.ReturnService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class ReturnController {

    private final ReturnService returnService;

    public ReturnController() {
        returnService = new ReturnService();
    }

    public void saveReturn(Return returnRecord) {
        returnService.saveReturn(returnRecord);
    }

    public ObservableList<Return> getReturns() {
        return FXCollections.observableArrayList(returnService.getAllReturns());
    }

    public ObservableList<Return> getReturnsForSale(int saleId) {
        return FXCollections.observableArrayList(returnService.getReturnsForSale(saleId));
    }

    public Return getReturnById(int returnId) {
        return returnService.getReturnById(returnId);
    }

    public List<ReturnableItem> getReturnableItems(int saleId) {
        return returnService.getReturnableItems(saleId);
    }
}
