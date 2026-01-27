package com.dsvviewer

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import javax.swing.Icon
import com.intellij.openapi.util.IconLoader

/**
 * Custom file type for Delimiter-Separated Values (DSV) files.
 * Supports CSV, TSV, and DSV file extensions.
 */
class DSVFileType private constructor() : LanguageFileType(PlainTextLanguage.INSTANCE) {

    override fun getName(): String = "DSV File"

    override fun getDescription(): String = "Delimiter-separated values file"

    override fun getDefaultExtension(): String = "csv"

    override fun getIcon(): Icon = ICON

    companion object {
        @JvmStatic
        val INSTANCE = DSVFileType()
        
        private val ICON: Icon by lazy {
            IconLoader.getIcon("/icons/dsv-file.svg", DSVFileType::class.java)
        }
    }
}
