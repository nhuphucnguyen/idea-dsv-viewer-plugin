package com.dsvviewer.ui

import com.dsvviewer.parser.ParsedData
import javax.swing.table.AbstractTableModel

/**
 * Table model for displaying DSV data in a JBTable.
 * Supports sorting and efficient data updates.
 */
class DSVTableModel : AbstractTableModel() {

    private var headers: List<String> = emptyList()
    private var rows: MutableList<MutableList<String>> = mutableListOf()
    private var sortColumn: Int = -1
    private var sortAscending: Boolean = true

    var onCellEdited: ((row: Int, col: Int, newValue: String) -> Unit)? = null

    fun updateData(data: ParsedData) {
        headers = data.headers
        rows = data.rows.map { it.toMutableList() }.toMutableList()
        sortColumn = -1
        fireTableStructureChanged()
    }

    fun clear() {
        headers = emptyList()
        rows = mutableListOf()
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

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        if (rowIndex < 0 || rowIndex >= rows.size) return
        val row = rows[rowIndex]
        if (columnIndex < 0 || columnIndex >= row.size) return
        val newValue = value?.toString() ?: ""
        if (row[columnIndex] != newValue) {
            row[columnIndex] = newValue
            fireTableCellUpdated(rowIndex, columnIndex)
            onCellEdited?.invoke(rowIndex, columnIndex, newValue)
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

    override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java

    fun sortByColumn(column: Int) {
        if (column < 0 || column >= headers.size) return

        sortAscending = if (sortColumn == column) !sortAscending else true
        sortColumn = column

        rows = if (sortAscending) {
            rows.sortedBy { it.getOrNull(column) ?: "" }.toMutableList()
        } else {
            rows.sortedByDescending { it.getOrNull(column) ?: "" }.toMutableList()
        }

        fireTableDataChanged()
    }

    fun getSortColumn(): Int = sortColumn

    fun isSortAscending(): Boolean = sortAscending

    fun getRowData(rowIndex: Int): List<String>? = rows.getOrNull(rowIndex)

    fun getHeaders(): List<String> = headers

    fun getRows(): List<List<String>> = rows.map { it.toList() }

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
