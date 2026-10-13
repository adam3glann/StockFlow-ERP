package com.adam.warehouse.model;

public record LowStockProduct(String name, int quantity, int minimumStock) {
}
