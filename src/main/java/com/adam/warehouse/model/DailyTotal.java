package com.adam.warehouse.model;

import java.time.LocalDate;

/** One day's sales and purchases totals, used to plot the dashboard trend chart. */
public record DailyTotal(LocalDate date, double sales, double purchases) {
}
