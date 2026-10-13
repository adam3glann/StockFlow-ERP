package com.adam.warehouse.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.adam.warehouse.util.MoneyValidation;

class TransactionModelTest {

    @Test
    void purchaseTotalIncludesAddedLinesAndDuplicateProducts() {
        Purchase purchase = new Purchase();
        purchase.addItem(new PurchaseItem(7, "Widget", 2, 12.50));
        purchase.addItem(new PurchaseItem(7, "Widget", 3, 12.50));

        assertEquals(62.50, purchase.getTotalAmount(), 0.001);
        assertEquals(5, purchase.getItems().getFirst().getQuantity());
    }

    @Test
    void persistedPurchaseTotalSurvivesListMappingWithoutLoadedItems() {
        Purchase purchase = new Purchase();
        purchase.setTotalAmount(145.75);

        assertEquals(145.75, purchase.getTotalAmount(), 0.001);
    }

    @Test
    void saleConsolidatesLinesAndUsesRecordedLineTotals() {
        Sale sale = new Sale();
        sale.addItem(new SaleItem(4, "Item", 2, 20.00, 13.00));
        sale.addItem(new SaleItem(4, "Item", 1, 20.00, 13.00));

        assertEquals(60.00, sale.getTotalAmount(), 0.001);
        assertEquals(3, sale.getItems().getFirst().getQuantity());
        assertEquals(21.00, sale.getItems().getFirst().getProfit(), 0.001);
    }

    @Test
    void emptyReportHasSafeAveragesAndRefundAdjustedGrossProfit() {
        ReportSummary summary = new ReportSummary();
        assertEquals(0, summary.getAverageSaleValue(), 0.001);
        assertEquals(0, summary.getProfitMargin(), 0.001);

        summary.setTotalProfit(100);
        summary.setTotalReturns(25);
        assertEquals(75, summary.getGrossProfitLessRefunds(), 0.001);
    }

    @Test
    void partialCustomerReturnCalculatesOnlyTheReturnedUnits() {
        Return returned = new Return();
        returned.addItem(new ReturnItem(11, 4, "Item", 2, 19.99, true));
        assertEquals(39.98, returned.getTotalRefund(), 0.001);
        assertEquals(2, returned.getTotalItems());
        assertTrue(returned.getItems().getFirst().isRestocked());
    }

    @Test
    void supplierReturnCreditUsesTheHistoricalPurchaseCost() {
        PurchaseReturn returned = new PurchaseReturn();
        returned.addItem(new PurchaseReturnItem(8, 4, "Item", 3, 12.25));
        assertEquals(36.75, returned.getTotalCredit(), 0.001);
    }

    @Test
    void stockAuditDistinguishesUnknownHistoryFromARealMismatch() {
        StockDiscrepancy row = new StockDiscrepancy();
        row.setCurrentQuantity(12);
        assertEquals("NO_HISTORY", row.getStatus());

        row.setHasMovementHistory(true);
        row.setLastRecordedQuantity(10);
        assertEquals("MISMATCH", row.getStatus());
        assertEquals(2, row.getDifference());

        row.setLastRecordedQuantity(12);
        assertEquals("CONSISTENT", row.getStatus());
    }

    @Test
    void monetaryInputsRejectNanInfinityAndValuesBeyondNumericColumnLimit() {
        assertTrue(MoneyValidation.isValidPositive(25.00));
        assertTrue(MoneyValidation.isValid(MoneyValidation.MAX_AMOUNT));
        assertFalse(MoneyValidation.isValid(Double.NaN));
        assertFalse(MoneyValidation.isValid(Double.POSITIVE_INFINITY));
        assertFalse(MoneyValidation.isValid(MoneyValidation.MAX_AMOUNT + 1000));
    }
}
