package com.dsvviewer.editor

import com.dsvviewer.DSVFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * FileEditorProvider for DSV files.
 * Creates DSVFileEditor instances for supported file types.
 */
class DSVFileEditorProvider : FileEditorProvider, DumbAware {

    companion object {
        private const val EDITOR_TYPE_ID = "dsv-viewer-editor"
        private val SUPPORTED_EXTENSIONS = setOf("csv", "tsv", "dsv")
    }

    override fun accept(project: Project, file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase() ?: return false
        return extension in SUPPORTED_EXTENSIONS || file.fileType is DSVFileType
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return DSVFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
