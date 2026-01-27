package com.dsvviewer.ui

import com.dsvviewer.parser.ParsedData
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.JTableHeader
import javax.swing.table.TableRowSorter

/**
 * Main table panel for displaying DSV data.
 * Features:
 * - Sticky header row
 * - Alternating row colors
 * - Column resizing
 * - Column sorting
 * - Cell tooltips
 * - Keyboard navigation
 * - Copy functionality
 */
class DSVTablePanel : JPanel(BorderLayout()) {

    private val tableModel = DSVTableModel()
    private val table: JBTable
    private val scrollPane: JBScrollPane
    private var searchHighlightRows: Set<Int> = emptySet()

    init {
        table = createTable()
        scrollPane = JBScrollPane(table)
        scrollPane.border = JBUI.Borders.empty()
        
        add(scrollPane, BorderLayout.CENTER)
        
        setupKeyboardShortcuts()
    }

    private fun createTable(): JBTable {
        return JBTable(tableModel).apply {
            // Enable auto-resize for columns
            autoResizeMode = JTable.AUTO_RESIZE_OFF
            
            // Set row height with proper scaling
            rowHeight = JBUI.scale(24)
            
            // Enable sorting
            rowSorter = TableRowSorter(tableModel)
            
            // Configure selection
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
            cellSelectionEnabled = true
            
            // Set column minimum width
            columnModel.columns.asSequence().forEach { column ->
                column.minWidth = JBUI.scale(50)
                column.preferredWidth = JBUI.scale(120)
            }
            
            // Custom cell renderer for alternating colors and tooltips
            setDefaultRenderer(Any::class.java, DSVCellRenderer())
            
            // Configure header
            tableHeader.apply {
                reorderingAllowed = true
                resizingAllowed = true
                defaultRenderer = DSVHeaderRenderer()
                
                // Add click listener for manual sorting
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        val column = columnAtPoint(e.point)
                        if (column >= 0) {
                            tableModel.sortByColumn(column)
                        }
                    }
                })
            }
            
            // Configure tooltip delay
            ToolTipManager.sharedInstance().initialDelay = 500
        }
    }

    private fun setupKeyboardShortcuts() {
        // Copy shortcut (Ctrl/Cmd+C)
        val copyKeyStroke = KeyStroke.getKeyStroke(
            KeyEvent.VK_C,
            Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        )
        
        table.inputMap.put(copyKeyStroke, "copy")
        table.actionMap.put("copy", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                copySelectedCells()
            }
        })

        // Select all shortcut (Ctrl/Cmd+A)  
        val selectAllKeyStroke = KeyStroke.getKeyStroke(
            KeyEvent.VK_A,
            Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        )
        
        table.inputMap.put(selectAllKeyStroke, "selectAll")
        table.actionMap.put("selectAll", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                table.selectAll()
            }
        })
    }

    /**
     * Updates the table with new parsed data.
     */
    fun updateData(data: ParsedData) {
        tableModel.updateData(data)
        adjustColumnWidths()
    }

    /**
     * Clears the table.
     */
    fun clear() {
        tableModel.clear()
    }

    /**
     * Adjusts column widths based on content.
     */
    private fun adjustColumnWidths() {
        val fontMetrics = table.getFontMetrics(table.font)
        val maxWidth = JBUI.scale(300)
        val minWidth = JBUI.scale(60)
        val padding = JBUI.scale(20)

        for (columnIndex in 0 until table.columnCount) {
            var preferredWidth = minWidth

            // Check header width
            val headerValue = table.getColumnName(columnIndex)
            val headerWidth = fontMetrics.stringWidth(headerValue) + padding
            preferredWidth = maxOf(preferredWidth, headerWidth)

            // Sample first 100 rows for content width
            val sampleSize = minOf(100, table.rowCount)
            for (rowIndex in 0 until sampleSize) {
                val value = table.getValueAt(rowIndex, columnIndex)?.toString() ?: ""
                val cellWidth = fontMetrics.stringWidth(value) + padding
                preferredWidth = maxOf(preferredWidth, minOf(cellWidth, maxWidth))
            }

            table.columnModel.getColumn(columnIndex).preferredWidth = preferredWidth
        }
    }

    /**
     * Copies selected cells to clipboard.
     */
    fun copySelectedCells() {
        val selectedRows = table.selectedRows
        val selectedColumns = table.selectedColumns

        if (selectedRows.isEmpty() || selectedColumns.isEmpty()) return

        val builder = StringBuilder()
        
        for (row in selectedRows) {
            val rowData = selectedColumns.map { col ->
                table.getValueAt(row, col)?.toString() ?: ""
            }
            builder.appendLine(rowData.joinToString("\t"))
        }

        val selection = StringSelection(builder.toString().trimEnd())
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    /**
     * Performs search and highlights matching rows.
     */
    fun search(query: String): Int {
        searchHighlightRows = if (query.isNotBlank()) {
            tableModel.searchRows(query).toSet()
        } else {
            emptySet()
        }
        table.repaint()
        
        // Scroll to first match
        if (searchHighlightRows.isNotEmpty()) {
            val firstMatch = searchHighlightRows.first()
            table.scrollRectToVisible(table.getCellRect(firstMatch, 0, true))
            table.setRowSelectionInterval(firstMatch, firstMatch)
        }
        
        return searchHighlightRows.size
    }

    /**
     * Gets the underlying table component.
     */
    fun getTable(): JBTable = table

    /**
     * Custom cell renderer with alternating row colors and search highlighting.
     */
    private inner class DSVCellRenderer : DefaultTableCellRenderer() {
        
        private val stripeColor = JBColor.namedColor(
            "Table.stripeColor", 
            JBColor(Color(245, 245, 245), Color(43, 45, 48))
        )
        
        private val searchHighlightColor = JBColor(
            Color(255, 255, 150),
            Color(100, 100, 50)
        )

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column
            )

            if (!isSelected) {
                background = when {
                    row in searchHighlightRows -> searchHighlightColor
                    row % 2 == 1 -> stripeColor
                    else -> table.background
                }
            }

            // Set tooltip for long text
            val text = value?.toString() ?: ""
            toolTipText = if (text.length > 50) text else null

            // Add padding
            border = JBUI.Borders.empty(2, 5)

            return component
        }
    }

    /**
     * Custom header renderer with sort indicator.
     */
    private inner class DSVHeaderRenderer : DefaultTableCellRenderer() {
        
        init {
            horizontalAlignment = SwingConstants.LEFT
        }

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column
            )

            // Style header
            background = UIUtil.getPanelBackground()
            foreground = UIUtil.getLabelForeground()
            font = font.deriveFont(Font.BOLD)
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 1),
                JBUI.Borders.empty(4, 5)
            )

            // Add sort indicator
            val sortColumn = tableModel.getSortColumn()
            if (column == sortColumn) {
                val arrow = if (tableModel.isSortAscending()) " ▲" else " ▼"
                text = "$value$arrow"
            }

            return component
        }
    }
}
