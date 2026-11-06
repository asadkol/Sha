package com.shahpharmacy.app.util

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ParsedPurchaseOrderRow(
    val name: String,
    val company: String = "",
    val qty: Int = 1,
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val batch: String = "",
    val expiry: String = "",
    val date: String = LocalDate.now().toString()
)

object ExcelPurchaseParser {

    /**
     * Parses an uploaded file from Uri, automatically detecting whether it is an
     * Excel (.xlsx) file or a CSV/TSV (.csv, .tsv, .txt) file.
     */
    fun parseFileUri(context: Context, uri: Uri): Result<List<ParsedPurchaseOrderRow>> {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val fileName = uri.lastPathSegment?.lowercase() ?: ""

            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open file stream"))

            if (fileName.endsWith(".xlsx") || mimeType.contains("spreadsheetml") || mimeType.contains("excel")) {
                parseXlsx(inputStream)
            } else {
                parseCsv(inputStream)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses CSV or TSV stream
     */
    fun parseCsv(inputStream: InputStream): Result<List<ParsedPurchaseOrderRow>> {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            parseCsvLines(lines)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses CSV lines text
     */
    fun parseCsvLines(lines: List<String>): Result<List<ParsedPurchaseOrderRow>> {
        if (lines.isEmpty()) return Result.failure(Exception("File is empty"))

        val resultList = mutableListOf<ParsedPurchaseOrderRow>()
        val todayStr = LocalDate.now().toString()

        var nameIdx = -1
        var compIdx = -1
        var qtyIdx = -1
        var purchIdx = -1
        var saleIdx = -1
        var batchIdx = -1
        var expIdx = -1
        var dateIdx = -1

        var headerFound = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val delimiter = if (line.contains("\t")) "\t" else if (line.contains(";")) ";" else ","
            val tokens = splitCsvLine(line, delimiter)
            if (tokens.isEmpty()) continue

            // Check if this line is header
            if (!headerFound) {
                var isHeader = false
                tokens.forEachIndexed { i, rawCol ->
                    val col = rawCol.trim().lowercase().replace("_", "").replace(" ", "").replace("-", "")
                    when {
                        col.contains("name") || col.contains("medicine") || col.contains("item") || col.contains("drug") -> {
                            nameIdx = i; isHeader = true
                        }
                        col.contains("comp") || col.contains("brand") || col.contains("mfg") || col.contains("supplier") -> {
                            compIdx = i; isHeader = true
                        }
                        col.contains("qty") || col.contains("quantity") || col.contains("count") || col.contains("units") -> {
                            qtyIdx = i; isHeader = true
                        }
                        col.contains("purchase") || col.contains("cost") || col.contains("buy") || col.contains("rate") -> {
                            purchIdx = i; isHeader = true
                        }
                        col.contains("sale") || col.contains("mrp") || col.contains("retail") || col.contains("selling") -> {
                            saleIdx = i; isHeader = true
                        }
                        col.contains("batch") || col.contains("lot") -> {
                            batchIdx = i; isHeader = true
                        }
                        col.contains("exp") || col.contains("expiry") -> {
                            expIdx = i; isHeader = true
                        }
                        col.contains("date") -> {
                            dateIdx = i; isHeader = true
                        }
                    }
                }

                if (isHeader && nameIdx != -1) {
                    headerFound = true
                    continue
                } else if (nameIdx == -1 && tokens.isNotEmpty()) {
                    // Default fallback positional: Name, Company, Qty, Purchase, Sale, Date, Batch, Expiry
                    nameIdx = 0
                    compIdx = if (tokens.size > 1) 1 else -1
                    qtyIdx = if (tokens.size > 2) 2 else -1
                    purchIdx = if (tokens.size > 3) 3 else -1
                    saleIdx = if (tokens.size > 4) 4 else -1
                    dateIdx = if (tokens.size > 5) 5 else -1
                    batchIdx = if (tokens.size > 6) 6 else -1
                    expIdx = if (tokens.size > 7) 7 else -1
                    headerFound = true
                }
            }

            // Extract row values
            val name = tokens.getOrNull(nameIdx)?.trim().orEmpty()
            if (name.isBlank() || name.equals("name", ignoreCase = true) || name.equals("medicine", ignoreCase = true)) {
                continue
            }

            val company = if (compIdx >= 0) tokens.getOrNull(compIdx)?.trim().orEmpty() else ""
            val qty = if (qtyIdx >= 0) tokens.getOrNull(qtyIdx)?.filter { it.isDigit() }?.toIntOrNull() ?: 1 else 1
            val purchase = if (purchIdx >= 0) tokens.getOrNull(purchIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0 else 0.0
            val sale = if (saleIdx >= 0) tokens.getOrNull(saleIdx)?.replace(",", "")?.toDoubleOrNull() ?: (purchase * 1.25) else (purchase * 1.25)
            val batch = if (batchIdx >= 0) tokens.getOrNull(batchIdx)?.trim().orEmpty() else ""
            val expiry = if (expIdx >= 0) tokens.getOrNull(expIdx)?.trim().orEmpty() else ""
            val date = if (dateIdx >= 0 && !tokens.getOrNull(dateIdx).isNullOrBlank()) tokens.getOrNull(dateIdx)!!.trim() else todayStr

            resultList.add(
                ParsedPurchaseOrderRow(
                    name = name,
                    company = company,
                    qty = qty.coerceAtLeast(1),
                    purchasePrice = purchase.coerceAtLeast(0.0),
                    salePrice = sale.coerceAtLeast(purchase),
                    batch = batch,
                    expiry = expiry,
                    date = date
                )
            )
        }

        return if (resultList.isNotEmpty()) {
            Result.success(resultList)
        } else {
            Result.failure(Exception("No valid medicine purchase rows could be parsed"))
        }
    }

    /**
     * Splits a single CSV line honoring quotes
     */
    private fun splitCsvLine(line: String, delimiter: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        val delimChar = delimiter[0]

        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delimChar && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Native zero-dependency .xlsx parser using ZipInputStream and XmlPullParser.
     * Excel files (.xlsx) are ZIP archives storing shared strings in `xl/sharedStrings.xml`
     * and row cell contents in `xl/worksheets/sheet1.xml`.
     */
    fun parseXlsx(inputStream: InputStream): Result<List<ParsedPurchaseOrderRow>> {
        return try {
            val bytes = inputStream.readBytes()
            val sharedStrings = mutableListOf<String>()
            val rawSheetRows = mutableListOf<Map<Int, String>>()

            // Step 1: Read sharedStrings.xml
            val zip1 = ZipInputStream(bytes.inputStream())
            var entry: ZipEntry? = zip1.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("sharedStrings.xml")) {
                    sharedStrings.addAll(parseSharedStrings(zip1))
                    break
                }
                zip1.closeEntry()
                entry = zip1.nextEntry
            }

            // Step 2: Read sheet1.xml
            val zip2 = ZipInputStream(bytes.inputStream())
            var entry2: ZipEntry? = zip2.nextEntry
            while (entry2 != null) {
                if (entry2.name.contains("sheet") && entry2.name.endsWith(".xml")) {
                    rawSheetRows.addAll(parseSheetXml(zip2, sharedStrings))
                    break
                }
                zip2.closeEntry()
                entry2 = zip2.nextEntry
            }

            // Step 3: Convert sheet rows to CSV format lines
            if (rawSheetRows.isEmpty()) {
                return Result.failure(Exception("No sheets or data found in Excel file"))
            }

            val maxCol = rawSheetRows.maxOfOrNull { row -> row.keys.maxOrNull() ?: 0 } ?: 0
            val csvLines = rawSheetRows.map { rowMap ->
                (0..maxCol).joinToString(",") { colIdx ->
                    val text = rowMap[colIdx].orEmpty().replace("\"", "\"\"")
                    "\"$text\""
                }
            }

            parseCsvLines(csvLines)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to read Excel (.xlsx) file: ${e.message}"))
        }
    }

    private fun parseSharedStrings(stream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var currentText = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = true
                    } else if (parser.name.equals("si", ignoreCase = true)) {
                        currentText.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("t", ignoreCase = true)) {
                        insideT = false
                    } else if (parser.name.equals("si", ignoreCase = true)) {
                        strings.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseSheetXml(stream: InputStream, sharedStrings: List<String>): List<Map<Int, String>> {
        val rows = mutableListOf<Map<Int, String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var currentRowMap = mutableMapOf<Int, String>()
        var currentCellCol = 0
        var isSharedString = false
        var insideV = false
        var cellValue = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (name.equals("row", ignoreCase = true)) {
                        currentRowMap = mutableMapOf()
                    } else if (name.equals("c", ignoreCase = true)) {
                        val ref = parser.getAttributeValue(null, "r").orEmpty()
                        currentCellCol = columnRefToIndex(ref)
                        val type = parser.getAttributeValue(null, "t").orEmpty()
                        isSharedString = (type == "s")
                        cellValue.setLength(0)
                    } else if (name.equals("v", ignoreCase = true) || name.equals("t", ignoreCase = true)) {
                        insideV = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) {
                        cellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name.equals("v", ignoreCase = true) || name.equals("t", ignoreCase = true)) {
                        insideV = false
                    } else if (name.equals("c", ignoreCase = true)) {
                        val raw = cellValue.toString().trim()
                        val finalVal = if (isSharedString) {
                            val idx = raw.toIntOrNull()
                            if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
                        } else {
                            raw
                        }
                        currentRowMap[currentCellCol] = finalVal
                    } else if (name.equals("row", ignoreCase = true)) {
                        if (currentRowMap.isNotEmpty()) {
                            rows.add(currentRowMap)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun columnRefToIndex(cellRef: String): Int {
        var colLetters = ""
        for (c in cellRef) {
            if (c.isLetter()) colLetters += c.uppercaseChar() else break
        }
        if (colLetters.isEmpty()) return 0
        var colIndex = 0
        for (c in colLetters) {
            colIndex = colIndex * 26 + (c - 'A' + 1)
        }
        return colIndex - 1
    }

    /**
     * Generates a sample CSV/Excel template string
     */
    fun getSampleTemplate(): String {
        return """
name,company,qty,purchase,sale,batch,expiry,date
Panadol 500mg,GSK,120,20.0,25.0,B-8821,2027-08-31,2026-10-13
Brufen 400mg,Abbott,80,45.0,55.0,LOT-441,2027-04-30,2026-10-13
Augmentin 625mg,GSK,50,185.0,220.0,AUG-992,2026-12-31,2026-10-13
Disprin 300mg,Reckitt,200,12.0,15.0,DSP-102,2028-01-31,2026-10-13
Flagyl 400mg,Sanofi,100,28.0,35.0,FLG-551,2027-09-30,2026-10-13
        """.trimIndent()
    }
}
