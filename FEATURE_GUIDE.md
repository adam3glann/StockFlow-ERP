# StockFlow ERP — New Features Guide

Launch StockFlow and log in.

## Barcode Scanner (visible sidebar screen)

1. Click **Barcode Scanner** in the left sidebar.
2. Scan with a USB/Bluetooth scanner, type a code and press Enter, or click **Scan Image** to choose a camera photo or screenshot.
3. The product, category, on-hand quantity, minimum stock and current stock status appear immediately.
4. When adding a product, use **Generate code** to create a unique StockFlow SKU if it does not already have a manufacturer barcode.
5. In **Products**, select a product and click **Barcode** to preview its label, print it, or save it as a PNG for a label printer.

The same scan methods are also available while recording a transaction:

- **Sales** → **New Sale** → **Add Item** → scan in the barcode field; set quantity and add.
- **Purchases** → **New Purchase** → **Add Item** → scan in the barcode field; set received quantity and add.

## Manual Stock Adjustment

1. Open **Inventory**.
2. Select a product.
3. Click **Adjust Stock** and enter a positive or negative quantity plus an optional reason.

4. Confirm the adjustment. The employee, reason, before/after balances, and timestamp are saved with the movement; stock cannot go below zero.

New products also create an opening-balance movement in the same transaction as the product row. Editing a product does not silently change its on-hand quantity; use Adjust Stock for later changes.

## Sales Return

1. Open **Returns** from the left sidebar and click **New Return**.
2. Select the original sale, choose the items and quantities to return, and enter an optional reason.
3. For each item, choose whether to restock it, then save the return.

The returned quantity cannot exceed the amount originally sold or the remaining returnable quantity. Restocked items increase inventory and create a `RETURN` movement.

## Reorder List (new)

1. Open **Inventory**.
2. Click **Export Reorder List**.
3. Save the CSV file.

It contains every low/out-of-stock product with barcode, category, on-hand quantity, minimum stock and a recommended reorder quantity.

## Purchase Return (new)

1. Open **Purchase Returns** from the left sidebar and click **New Purchase Return**.
2. Select the original purchase, choose the items and quantities to send back, and enter an optional reason.
3. Save the return.

The returned quantity cannot exceed the remaining quantity from the purchase or the amount currently on hand. Stock is withdrawn and a `PURCHASE_RETURN` movement is recorded. Use **View** on a purchase to see its line items without exporting an invoice.

## Stock Audit

1. Open **Inventory** and click **Stock Audit**.
2. Search by product name, barcode, or category, and filter by status.
3. Export the filtered rows to CSV for investigation.

Compares each active product's current on-hand quantity against the ending quantity of its most recent recorded inventory movement (purchase, sale, adjustment, customer return, or supplier return). The result shows movement type, reference, timestamp, last recorded quantity, current quantity, and difference. Products without movement history are marked **No movement history** and are excluded from discrepancy claims; this can occur with records created before opening-balance tracking was added. The audit never changes stock.

This check can identify unrecorded database edits and some software/ledger inconsistencies. It cannot detect a physical item leaving without being scanned; use the physical-count workflow below to compare actual stock and investigate any variance.

## Physical Stock Count

1. In **Inventory**, choose **Physical Count**. Resume an open count or start a new session; the app snapshots recorded quantities for active products.
2. Double-click the **Counted Quantity** cell for each product and enter the physical count. For any difference, enter a count reason before completing the session.
3. Open **Review Count Variances**, select a variance, record the investigation result, and confirm **Investigate and Resolve** to apply the counted quantity.
4. Open **Physical Count History** to review counts, variances, the resolving employee, timestamp, and resolution notes. An unfinished session can be discarded; it remains in history and does not alter stock.

Completing a count records evidence but does not adjust inventory. Resolution creates an audited stock movement in the same transaction as the product quantity update. If that product has moved since the count began, the app blocks resolution and requires a recount. The system still cannot detect physical loss without a count or external hardware such as a scanner, gate sensor, or RFID integration.
