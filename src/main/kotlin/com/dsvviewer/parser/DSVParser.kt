package com.dsvviewer.parser

/**
 * Robust DSV (Delimiter-Separated Values) parser with support for:
 * - Quote-aware field parsing (handles fields with delimiters inside quotes)
 * - Escaped quote handling ("" inside quoted fields)
 * - Multi-line fields within quotes
 * - Row normalization (consistent column count)
 * - Whitespace trimming for unquoted fields
 */
class DSVParser {

    /**
     * Parses DSV content with the specified delimiter and header settings.
     * 
     * @param content The raw file content to parse
     * @param delimiter The delimiter character used to separate fields
     * @param hasHeader Whether the first row should be treated as headers
     * @return ParsedData containing headers, rows, and metadata
     */
    fun parse(content: String, delimiter: Char, hasHeader: Boolean): ParsedData {
        if (content.isBlank()) {
            return ParsedData.empty()
        }

        val allRows = parseRows(content, delimiter)
        
        if (allRows.isEmpty()) {
            return ParsedData.empty()
        }

        // Determine max column count for normalization
        val maxColumns = allRows.maxOfOrNull { it.size } ?: 0

        // Normalize all rows to have the same column count
        val normalizedRows = allRows.map { row ->
            if (row.size < maxColumns) {
                row + List(maxColumns - row.size) { "" }
            } else {
                row
            }
        }

        val (headers, dataRows) = if (hasHeader && normalizedRows.isNotEmpty()) {
            val headerRow = normalizedRows.first()
            val data = normalizedRows.drop(1)
            headerRow to data
        } else {
            val generatedHeaders = (1..maxColumns).map { "Column $it" }
            generatedHeaders to normalizedRows
        }

        return ParsedData(
            headers = headers,
            rows = dataRows,
            columnCount = maxColumns,
            rowCount = dataRows.size
        )
    }

    /**
     * Parses the content into rows of fields, handling quotes and multi-line fields.
     */
    private fun parseRows(content: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        val currentRow = mutableListOf<String>()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val char = content[i]
            val nextChar = content.getOrNull(i + 1)

            when {
                // Start or end of quoted field
                char == '"' && !inQuotes -> {
                    inQuotes = true
                }
                
                // Escaped quote ("") inside quoted field
                char == '"' && inQuotes && nextChar == '"' -> {
                    currentField.append('"')
                    i++ // Skip the next quote
                }
                
                // End of quoted field
                char == '"' && inQuotes -> {
                    inQuotes = false
                }
                
                // Delimiter outside of quotes
                char == delimiter && !inQuotes -> {
                    currentRow.add(currentField.toString().let { 
                        if (!inQuotes) it.trim() else it 
                    })
                    currentField.clear()
                }
                
                // Handle line endings
                char == '\r' && nextChar == '\n' && !inQuotes -> {
                    // CRLF line ending
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                    if (currentRow.isNotEmpty() || rows.isNotEmpty()) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                    i++ // Skip the \n
                }
                
                char == '\n' && !inQuotes -> {
                    // LF line ending
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                    if (currentRow.isNotEmpty() || rows.isNotEmpty()) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                
                char == '\r' && !inQuotes -> {
                    // CR line ending (old Mac)
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                    if (currentRow.isNotEmpty() || rows.isNotEmpty()) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                
                // Regular character
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }

        // Handle last field and row
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            rows.add(currentRow.toList())
        }

        return rows
    }

    /**
     * Auto-detects the delimiter based on file extension and content analysis.
     */
    fun detectDelimiter(content: String, fileName: String? = null): Char {
        // Check file extension first
        fileName?.let { name ->
            when {
                name.endsWith(".tsv", ignoreCase = true) -> return '\t'
                name.endsWith(".csv", ignoreCase = true) -> return ','
                else -> { /* continue to content analysis */ }
            }
        }

        // Analyze content to detect delimiter
        val firstLine = content.lineSequence().firstOrNull() ?: return ','
        
        val delimiters = listOf(',', '\t', ';', '|', ' ')
        val counts = delimiters.associateWith { delimiter ->
            countDelimiterOccurrences(firstLine, delimiter)
        }

        // Return delimiter with highest count, defaulting to comma
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ','
    }

    /**
     * Counts occurrences of a delimiter in a line, respecting quoted fields.
     */
    private fun countDelimiterOccurrences(line: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> count++
                else -> { /* ignore other characters */ }
            }
        }
        
        return count
    }

    companion object {
        val COMMON_DELIMITERS = listOf(
            DelimiterOption(',', "Comma (,)"),
            DelimiterOption('\t', "Tab (\\t)"),
            DelimiterOption(';', "Semicolon (;)"),
            DelimiterOption('|', "Pipe (|)"),
            DelimiterOption(' ', "Space")
        )
    }
}

/**
 * Represents a delimiter option for the UI.
 */
data class DelimiterOption(
    val character: Char,
    val displayName: String
) {
    override fun toString(): String = displayName
}
