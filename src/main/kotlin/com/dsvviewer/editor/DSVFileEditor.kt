package com.dsvviewer.editor

import com.dsvviewer.parser.DSVParser
import com.dsvviewer.parser.ParsedData
import com.dsvviewer.ui.DSVEditorToolbar
import com.dsvviewer.ui.DSVTablePanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.CardLayout
import java.beans.PropertyChangeListener
import java.io.File
import java.io.FileWriter
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Custom file editor for DSV files.
 * Displays content in an interactive table view with toolbar controls.
 */
class DSVFileEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    private val parser = DSVParser()
    private var currentData: ParsedData = ParsedData.empty()
    private var isTableView = true

    // UI Components
    private val mainPanel = JPanel(BorderLayout())
    private val contentPanel = JPanel(CardLayout())
    private val tablePanel = DSVTablePanel()
    private val toolbar: DSVEditorToolbar
    private var textEditor: EditorEx? = null

    companion object {
        private const val TABLE_VIEW = "TABLE"
        private const val TEXT_VIEW = "TEXT"
    }

    init {
        toolbar = DSVEditorToolbar(
            onDelimiterChanged = { reparse() },
            onHeaderToggled = { reparse() },
            onZeroBasedToggled = { reparse() },
            onRefresh = { reparse() },
            onExport = { exportToCsv() },
            onToggleView = { toggleView() },
            onSearch = { query -> tablePanel.search(query) }
        )

        // Set up content panel with card layout
        contentPanel.add(tablePanel, TABLE_VIEW)
        
        // Main layout
        mainPanel.add(toolbar, BorderLayout.NORTH)
        mainPanel.add(contentPanel, BorderLayout.CENTER)

        // Auto-detect delimiter and parse initially
        detectDelimiterAndParse()
    }

    private fun detectDelimiterAndParse() {
        val content = readFileContent()
        val detectedDelimiter = parser.detectDelimiter(content, file.name)
        toolbar.setDelimiter(detectedDelimiter)
        parseInBackground(content)
    }

    private fun readFileContent(): String {
        return try {
            String(file.contentsToByteArray(), file.charset)
        } catch (e: Exception) {
            ""
        }
    }

    private fun reparse() {
        val content = readFileContent()
        parseInBackground(content)
    }

    private fun parseInBackground(content: String) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Parsing DSV File", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Parsing ${file.name}..."
                indicator.isIndeterminate = true

                val delimiter = toolbar.getDelimiter()
                val hasHeader = toolbar.hasHeader()
                val isZeroBased = toolbar.isZeroBased()
                val parsedData = parser.parse(content, delimiter, hasHeader, isZeroBased)

                // Update UI on EDT
                SwingUtilities.invokeLater {
                    currentData = parsedData
                    tablePanel.updateData(parsedData)
                    toolbar.updateInfo(
                        parsedData.rowCount,
                        parsedData.columnCount,
                        file.length
                    )
                }
            }
        })
    }

    private fun exportToCsv() {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Export as CSV"
            fileFilter = FileNameExtensionFilter("CSV Files (*.csv)", "csv")
            selectedFile = File(file.nameWithoutExtension + "_export.csv")
        }

        if (fileChooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            var exportFile = fileChooser.selectedFile
            if (!exportFile.name.endsWith(".csv")) {
                exportFile = File(exportFile.absolutePath + ".csv")
            }

            try {
                FileWriter(exportFile).use { writer ->
                    // Write headers
                    writer.write(currentData.headers.joinToString(",") { escapeForCsv(it) })
                    writer.write("\n")
                    
                    // Write data rows
                    for (row in currentData.rows) {
                        writer.write(row.joinToString(",") { escapeForCsv(it) })
                        writer.write("\n")
                    }
                }
                
                JOptionPane.showMessageDialog(
                    mainPanel,
                    "File exported successfully to:\n${exportFile.absolutePath}",
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    mainPanel,
                    "Failed to export file: ${e.message}",
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }

    private fun escapeForCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun toggleView() {
        isTableView = !isTableView
        
        if (isTableView) {
            // Switch to table view
            (contentPanel.layout as CardLayout).show(contentPanel, TABLE_VIEW)
        } else {
            // Switch to text view - create editor if needed
            if (textEditor == null) {
                createTextEditor()
            }
            (contentPanel.layout as CardLayout).show(contentPanel, TEXT_VIEW)
        }
    }

    private fun createTextEditor() {
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument(readFileContent())
        textEditor = editorFactory.createEditor(document, project, file, true) as? EditorEx
        
        textEditor?.let { editor ->
            editor.settings.apply {
                isLineNumbersShown = true
                isWhitespacesShown = false
                isFoldingOutlineShown = true
                isRightMarginShown = true
            }
            
            val scrollPane = JBScrollPane(editor.component)
            contentPanel.add(scrollPane, TEXT_VIEW)
        }
    }

    override fun getComponent() = mainPanel

    override fun getPreferredFocusedComponent() = tablePanel.getTable()

    override fun getName() = "DSV Viewer"

    override fun setState(state: FileEditorState) {}

    override fun isModified() = false

    override fun isValid() = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        textEditor?.let { editor ->
            ApplicationManager.getApplication().runWriteAction {
                EditorFactory.getInstance().releaseEditor(editor)
            }
        }
    }

    override fun getFile(): VirtualFile = file
}
