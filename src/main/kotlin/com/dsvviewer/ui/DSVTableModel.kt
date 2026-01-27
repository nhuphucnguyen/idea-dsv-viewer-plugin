package com.dsvviewer.ui

import com.dsvviewer.parser.ParsedData
import javax.swing.table.AbstractTableModel

/**
 * Table model for displaying DSV data in a JBTable.
 * Supports sorting and efficient data updates.
 */
class DSVTableModel : AbstractTableModel() {

    private var headers: List<String> = emptyList()
    private var rows: List<List<String>> = emptyList()
    private var sortColumn: Int = -1
    private var sortAscending: Boolean = true

    /**
     * Updates the model with new parsed data.
     */
    fun updateData(data: ParsedData) {
        headers = data.headers
        rows = data.rows
        sortColumn = -1
        fireTableStructureChanged()
    }

    /**
     * Clears all data from the model.
     */
    fun clear() {
        headers = emptyList()
        rows = emptyList()
        sortColumn = -1
        fireTableStructureChanged()
    }

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = headers.size

    override fun getColumnName(column: Int): String {
        return if (column in headers.indices) headers[column] else ""
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        return rows.getOrNull(rowIndex)?.getOrNull(columnIndex) ?: ""
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

    override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java

    /**
     * Sorts the table by the specified column.
     */
    fun sortByColumn(column: Int) {
        if (column < 0 || column >= headers.size) return

        // Toggle sort direction if clicking the same column
        sortAscending = if (sortColumn == column) !sortAscending else true
        sortColumn = column

        rows = if (sortAscending) {
            rows.sortedBy { it.getOrNull(column) ?: "" }
        } else {
            rows.sortedByDescending { it.getOrNull(column) ?: "" }
        }

        fireTableDataChanged()
    }

    /**
     * Gets the current sort column index.
     */
    fun getSortColumn(): Int = sortColumn

    /**
     * Returns whether the current sort is ascending.
     */
    fun isSortAscending(): Boolean = sortAscending

    /**
     * Gets the raw row data at the specified index.
     */
    fun getRowData(rowIndex: Int): List<String>? = rows.getOrNull(rowIndex)

    /**
     * Gets all rows matching a search query.
     */
    fun searchRows(query: String, caseSensitive: Boolean = false): List<Int> {
        if (query.isBlank()) return emptyList()
        
        val searchQuery = if (caseSensitive) query else query.lowercase()
        
        return rows.mapIndexedNotNull { index, row ->
            val matches = row.any { cell ->
                val cellValue = if (caseSensitive) cell else cell.lowercase()
                cellValue.contains(searchQuery)
            }
            if (matches) index else null
        }
    }
}
