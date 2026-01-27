package com.dsvviewer.parser

/**
 * Data class representing parsed DSV data.
 */
data class ParsedData(
    val headers: List<String>,
    val rows: List<List<String>>,
    val columnCount: Int,
    val rowCount: Int
) {
    companion object {
        fun empty(): ParsedData = ParsedData(
            headers = emptyList(),
            rows = emptyList(),
            columnCount = 0,
            rowCount = 0
        )
    }
}
