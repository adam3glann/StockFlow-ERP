package com.adam.warehouse.model;

/** Aggregated sales performance for a single product, used by dashboard and reports. */
public record TopSellingProduct(String name, int quantitySold, double revenue) {
}
