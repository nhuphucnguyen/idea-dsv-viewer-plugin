package com.dsvviewer.ui

import com.dsvviewer.parser.DSVParser
import com.dsvviewer.parser.DelimiterOption
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent

/**
 * Toolbar component for the DSV editor.
 * Contains:
 * - Delimiter selector dropdown
 * - Header row toggle checkbox
 * - Refresh, Export, Toggle View buttons
 * - Search field
 * - Info display (row count, column count, file size)
 */
class DSVEditorToolbar(
    private val onDelimiterChanged: (Char) -> Unit,
    private val onHeaderToggled: (Boolean) -> Unit,
    private val onRefresh: () -> Unit,
    private val onExport: () -> Unit,
    private val onToggleView: () -> Unit,
    private val onSearch: (String) -> Unit
) : JPanel() {

    private val delimiterComboBox: ComboBox<DelimiterOption>
    private val headerCheckBox: JBCheckBox
    private val searchField: SearchTextField
    private val infoLabel: JBLabel
    
    private var currentDelimiter: Char = ','
    private var isUpdatingDelimiter = false

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = JBUI.Borders.empty(4, 8)

        // Delimiter selector
        val delimiterLabel = JBLabel("Delimiter:")
        delimiterLabel.border = JBUI.Borders.emptyRight(4)
        add(delimiterLabel)

        val delimiterOptions = DSVParser.COMMON_DELIMITERS.toTypedArray()
        delimiterComboBox = ComboBox(delimiterOptions)
        delimiterComboBox.maximumSize = delimiterComboBox.preferredSize
        delimiterComboBox.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED && !isUpdatingDelimiter) {
                val selected = delimiterComboBox.selectedItem as? DelimiterOption
                selected?.let { 
                    currentDelimiter = it.character
                    onDelimiterChanged(it.character) 
                }
            }
        }
        add(delimiterComboBox)

        // Custom delimiter button
        val customDelimiterBtn = JButton("Custom...")
        customDelimiterBtn.addActionListener { showCustomDelimiterDialog() }
        customDelimiterBtn.margin = JBUI.insets(2, 8)
        add(Box.createHorizontalStrut(4))
        add(customDelimiterBtn)

        add(Box.createHorizontalStrut(16))

        // Header checkbox
        headerCheckBox = JBCheckBox("File has header row", true)
        headerCheckBox.addItemListener { 
            onHeaderToggled(headerCheckBox.isSelected)
        }
        add(headerCheckBox)

        add(Box.createHorizontalStrut(16))

        // Action buttons panel
        val actionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        actionsPanel.isOpaque = false

        val refreshBtn = createIconButton("Refresh", AllIcons.Actions.Refresh) { onRefresh() }
        actionsPanel.add(refreshBtn)

        val exportBtn = createIconButton("Export as CSV", AllIcons.ToolbarDecorator.Export) { onExport() }
        actionsPanel.add(exportBtn)

        val toggleViewBtn = createIconButton("Switch to Text View", AllIcons.Actions.ToggleSoftWrap) { onToggleView() }
        actionsPanel.add(toggleViewBtn)

        add(actionsPanel)

        add(Box.createHorizontalStrut(16))

        // Search field
        val searchLabel = JBLabel("Search:")
        searchLabel.border = JBUI.Borders.emptyRight(4)
        add(searchLabel)
        
        searchField = SearchTextField(true)
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                onSearch(searchField.text)
            }
        })
        searchField.maximumSize = searchField.preferredSize
        add(searchField)

        add(Box.createHorizontalGlue())

        // Info display
        infoLabel = JBLabel()
        infoLabel.border = JBUI.Borders.emptyLeft(16)
        add(infoLabel)
    }

    private fun createIconButton(tooltip: String, icon: Icon, action: () -> Unit): JButton {
        return JButton(icon).apply {
            toolTipText = tooltip
            isBorderPainted = false
            isContentAreaFilled = false
            margin = JBUI.emptyInsets()
            addActionListener { action() }
        }
    }

    private fun showCustomDelimiterDialog() {
        val result = JOptionPane.showInputDialog(
            this,
            "Enter custom delimiter character:",
            "Custom Delimiter",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            currentDelimiter.toString()
        )
        
        if (result is String && result.isNotEmpty()) {
            val customChar = result[0]
            currentDelimiter = customChar
            
            // Add custom option to combo box
            isUpdatingDelimiter = true
            val customOption = DelimiterOption(customChar, "Custom: '$customChar'")
            delimiterComboBox.addItem(customOption)
            delimiterComboBox.selectedItem = customOption
            isUpdatingDelimiter = false
            
            onDelimiterChanged(customChar)
        }
    }

    /**
     * Updates the info display with current file statistics.
     */
    fun updateInfo(rowCount: Int, columnCount: Int, fileSize: Long) {
        val sizeStr = formatFileSize(fileSize)
        infoLabel.text = "$rowCount rows × $columnCount columns | $sizeStr"
    }

    /**
     * Sets the delimiter in the combo box.
     */
    fun setDelimiter(delimiter: Char) {
        isUpdatingDelimiter = true
        currentDelimiter = delimiter
        
        // Find matching option or add custom
        val existingOption = DSVParser.COMMON_DELIMITERS.find { it.character == delimiter }
        if (existingOption != null) {
            delimiterComboBox.selectedItem = existingOption
        } else {
            val customOption = DelimiterOption(delimiter, "Custom: '$delimiter'")
            delimiterComboBox.addItem(customOption)
            delimiterComboBox.selectedItem = customOption
        }
        
        isUpdatingDelimiter = false
    }

    /**
     * Sets the header checkbox state.
     */
    fun setHasHeader(hasHeader: Boolean) {
        headerCheckBox.isSelected = hasHeader
    }

    /**
     * Gets the current delimiter.
     */
    fun getDelimiter(): Char = currentDelimiter

    /**
     * Gets whether the file has a header row.
     */
    fun hasHeader(): Boolean = headerCheckBox.isSelected

    /**
     * Clears the search field.
     */
    fun clearSearch() {
        searchField.text = ""
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
