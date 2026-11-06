package com.shahpharmacy.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.shahpharmacy.app.data.model.Purchase
import com.shahpharmacy.app.data.model.StockItem
import com.shahpharmacy.app.ui.components.formatMoney
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ThermalPrinterHelper {

    private const val LINE_WIDTH_58MM = 32

    private fun line(char: Char = '-', width: Int = LINE_WIDTH_58MM): String {
        return char.toString().repeat(width)
    }

    private fun center(text: String, width: Int = LINE_WIDTH_58MM): String {
        if (text.length >= width) return text.take(width)
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun twoCols(left: String, right: String, width: Int = LINE_WIDTH_58MM): String {
        val total = width - right.length
        return if (left.length <= total) {
            left + " ".repeat(total - left.length) + right
        } else {
            left.take(total - 1) + " " + right
        }
    }

    /**
     * Generates a thermal receipt string for Low Stock / Purchase Reorder Order
     */
    fun generateLowStockOrderReceipt(
        lowStockItems: List<StockItem>,
        threshold: Int
    ): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH))
        val sb = StringBuilder()

        sb.appendLine(center("SHAH PHARMACY"))
        sb.appendLine(center("Main Bazar, Medical Market"))
        sb.appendLine(center("PURCHASE REORDER SLIP"))
        sb.appendLine(line('='))
        sb.appendLine(twoCols("Date:", now))
        sb.appendLine(twoCols("Reorder Threshold:", "<= $threshold units"))
        sb.appendLine(twoCols("Items to Reorder:", "${lowStockItems.size} items"))
        sb.appendLine(line('-'))
        sb.appendLine(twoCols("ITEM / COMPANY", "STOCK -> ORDER"))
        sb.appendLine(line('-'))

        var totalEstimatedCost = 0.0

        lowStockItems.forEachIndexed { index, item ->
            val suggestedOrder = if (item.qty <= 0) threshold * 3 else (threshold * 2 - item.qty).coerceAtLeast(threshold)
            val estCost = suggestedOrder * item.purchase
            totalEstimatedCost += estCost

            val num = "${index + 1}. ${item.name}"
            sb.appendLine(if (num.length > LINE_WIDTH_58MM) num.take(LINE_WIDTH_58MM) else num)
            val sub = if (item.company.isNotBlank()) "   [${item.company}]" else "   "
            val qtyCol = "${item.qty} -> $suggestedOrder pcs"
            sb.appendLine(twoCols(sub, qtyCol))
            if (item.purchase > 0) {
                sb.appendLine(twoCols("   @ Rs.${item.purchase.toInt()}", "Est: Rs.${estCost.toInt()}"))
            }
        }

        sb.appendLine(line('='))
        sb.appendLine(twoCols("TOTAL EST. ORDER:", "Rs. ${totalEstimatedCost.toInt()}"))
        sb.appendLine(line('-'))
        sb.appendLine("")
        sb.appendLine(center("Distributor / Supplier Copy"))
        sb.appendLine(center("Authorized Signature:"))
        sb.appendLine("")
        sb.appendLine(center("__________________________"))
        sb.appendLine(center("*** THANK YOU ***"))
        sb.appendLine("")
        sb.appendLine("")

        return sb.toString()
    }

    /**
     * Generates a thermal receipt for full inventory audit
     */
    fun generateInventoryAuditReceipt(stockItems: List<StockItem>): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH))
        val sb = StringBuilder()

        sb.appendLine(center("SHAH PHARMACY"))
        sb.appendLine(center("INVENTORY STOCK AUDIT"))
        sb.appendLine(line('='))
        sb.appendLine(twoCols("Date:", now))
        sb.appendLine(twoCols("Total Medicine Lines:", "${stockItems.size}"))
        sb.appendLine(line('-'))
        sb.appendLine(twoCols("MEDICINE", "QTY | COST"))
        sb.appendLine(line('-'))

        var totalQty = 0
        var totalVal = 0.0

        stockItems.forEachIndexed { idx, it ->
            totalQty += it.qty
            totalVal += it.purchaseValue
            val name = "${idx + 1}. ${it.name}"
            sb.appendLine(if (name.length > LINE_WIDTH_58MM) name.take(LINE_WIDTH_58MM) else name)
            sb.appendLine(twoCols("   ${it.company}", "${it.qty}x Rs.${it.purchase.toInt()}"))
        }

        sb.appendLine(line('='))
        sb.appendLine(twoCols("TOTAL UNITS:", "$totalQty pcs"))
        sb.appendLine(twoCols("STOCK VALUATION:", "Rs. ${totalVal.toInt()}"))
        sb.appendLine(line('-'))
        sb.appendLine(center("*** END OF AUDIT ***"))
        sb.appendLine("")
        sb.appendLine("")

        return sb.toString()
    }

    /**
     * Generates a thermal receipt for a single purchase entry
     */
    fun generatePurchaseReceipt(purchase: Purchase): String {
        val sb = StringBuilder()
        sb.appendLine(center("SHAH PHARMACY"))
        sb.appendLine(center("PURCHASE INWARD VOUCHER"))
        sb.appendLine(line('='))
        sb.appendLine(twoCols("Date:", purchase.date))
        sb.appendLine(twoCols("Medicine:", purchase.name))
        if (purchase.company.isNotBlank()) sb.appendLine(twoCols("Company:", purchase.company))
        if (purchase.batch.isNotBlank()) sb.appendLine(twoCols("Batch #:", purchase.batch))
        if (purchase.expiry.isNotBlank()) sb.appendLine(twoCols("Expiry:", purchase.expiry))
        sb.appendLine(line('-'))
        sb.appendLine(twoCols("Quantity:", "${purchase.qty} units"))
        sb.appendLine(twoCols("Unit Purchase:", "Rs. ${purchase.purchase.toInt()}"))
        sb.appendLine(twoCols("Unit Sale Price:", "Rs. ${purchase.sale.toInt()}"))
        sb.appendLine(line('='))
        sb.appendLine(twoCols("TOTAL PURCHASE:", "Rs. ${purchase.total.toInt()}"))
        sb.appendLine(line('-'))
        sb.appendLine(center("Recorded in System"))
        sb.appendLine(center("*** SHAH PHARMACY ***"))
        sb.appendLine("")
        return sb.toString()
    }

    /**
     * Copies raw thermal monospace text to Android clipboard
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Thermal Receipt") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Thermal slip copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Shares thermal receipt via WhatsApp / Bluetooth / SMS / Email
     */
    fun shareReceipt(context: Context, text: String, title: String = "Shah Pharmacy Thermal Slip") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Thermal Slip"))
    }

    /**
     * Prints thermal receipt using Android PrintManager via WebView HTML rendering.
     * Works with any ESC/POS thermal printer, Wi-Fi printer, Bluetooth printer, or saves to PDF.
     */
    fun printReceipt(context: Context, receiptText: String, jobName: String = "Shah_Pharmacy_Receipt") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                copyToClipboard(context, receiptText)
                return
            }

            val webView = WebView(context)
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        @page {
                            margin: 4mm;
                            size: 58mm auto;
                        }
                        body {
                            font-family: 'Courier New', Courier, monospace;
                            font-size: 11px;
                            line-height: 1.25;
                            color: #000;
                            margin: 0;
                            padding: 0;
                            width: 54mm;
                            white-space: pre-wrap;
                            word-break: break-all;
                        }
                    </style>
                </head>
                <body>$receiptText</body>
                </html>
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                        .setResolution(PrintAttributes.Resolution("thermal", "Thermal", 203, 203))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } catch (e: Exception) {
            copyToClipboard(context, receiptText)
            Toast.makeText(context, "Printing error: ${e.message}. Copied to clipboard.", Toast.LENGTH_LONG).show()
        }
    }
}
