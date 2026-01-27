package com.dsvviewer.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Unit tests for DSVParser covering various edge cases.
 */
class DSVParserTest {

    private val parser = DSVParser()

    @Test
    @DisplayName("Parse simple CSV with header")
    fun testSimpleCsvWithHeader() {
        val content = "Name,Age,City\nJohn,30,New York\nJane,25,London"

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals(listOf("Name", "Age", "City"), result.headers)
        assertEquals(2, result.rowCount)
        assertEquals(3, result.columnCount)
        assertEquals("John", result.rows[0][0])
        assertEquals("30", result.rows[0][1])
    }

    @Test
    @DisplayName("Parse CSV without header")
    fun testCsvWithoutHeader() {
        val content = "John,30,New York\nJane,25,London"

        val result = parser.parse(content, ',', hasHeader = false)

        assertEquals(listOf("Column 1", "Column 2", "Column 3"), result.headers)
        assertEquals(2, result.rowCount)
    }

    @Test
    @DisplayName("Parse quoted fields with delimiter inside")
    fun testQuotedFieldsWithDelimiter() {
        val content = "Name,Description\n\"John, Jr.\",A person\nJane,\"Lives in London, UK\""

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals("John, Jr.", result.rows[0][0])
        assertEquals("Lives in London, UK", result.rows[1][1])
    }

    @Test
    @DisplayName("Parse escaped quotes inside quoted fields")
    fun testEscapedQuotes() {
        val content = "Name,Quote\nJohn,\"He said \"\"hello\"\"\"\nJane,\"She replied \"\"hi\"\" back\""

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals("He said \"hello\"", result.rows[0][1])
        assertEquals("She replied \"hi\" back", result.rows[1][1])
    }

    @Test
    @DisplayName("Parse multi-line fields within quotes")
    fun testMultiLineFields() {
        val content = "Name,Description\nJohn,\"Line 1\nLine 2\nLine 3\"\nJane,Simple"

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals("Line 1\nLine 2\nLine 3", result.rows[0][1])
        assertEquals("Simple", result.rows[1][1])
    }

    @Test
    @DisplayName("Parse empty fields")
    fun testEmptyFields() {
        val content = "A,B,C\n1,,3\n,2,"

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals("1", result.rows[0][0])
        assertEquals("", result.rows[0][1])
        assertEquals("3", result.rows[0][2])
        assertEquals("", result.rows[1][0])
        assertEquals("2", result.rows[1][1])
    }

    @Test
    @DisplayName("Normalize rows with inconsistent column counts")
    fun testRowNormalization() {
        val content = "A,B,C\n1,2\n1,2,3,4"

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals(4, result.columnCount)
        assertEquals(4, result.rows[0].size)
        assertEquals(4, result.rows[1].size)
        assertEquals("", result.rows[0][2])
        assertEquals("", result.rows[0][3])
    }

    @Test
    @DisplayName("Parse TSV (tab-separated)")
    fun testTsvParsing() {
        val content = "Name\tAge\tCity\nJohn\t30\tNew York"

        val result = parser.parse(content, '\t', hasHeader = true)

        assertEquals(listOf("Name", "Age", "City"), result.headers)
        assertEquals("John", result.rows[0][0])
    }

    @Test
    @DisplayName("Parse with semicolon delimiter")
    fun testSemicolonDelimiter() {
        val content = "Name;Age;City\nJohn;30;New York"

        val result = parser.parse(content, ';', hasHeader = true)

        assertEquals(listOf("Name", "Age", "City"), result.headers)
    }

    @Test
    @DisplayName("Handle CRLF line endings")
    fun testCrlfLineEndings() {
        val content = "Name,Age\r\nJohn,30\r\nJane,25"

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals(2, result.rowCount)
        assertEquals("John", result.rows[0][0])
        assertEquals("Jane", result.rows[1][0])
    }

    @Test
    @DisplayName("Handle empty content")
    fun testEmptyContent() {
        val result = parser.parse("", ',', hasHeader = true)

        assertEquals(ParsedData.empty(), result)
    }

    @Test
    @DisplayName("Handle whitespace-only content")
    fun testWhitespaceContent() {
        val result = parser.parse("   \n  \n  ", ',', hasHeader = true)

        assertEquals(0, result.rowCount)
    }

    @Test
    @DisplayName("Parse Unicode characters")
    fun testUnicodeCharacters() {
        val content = "名前,年齢,都市\n太郎,30,東京\n花子,25,大阪"

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals("名前", result.headers[0])
        assertEquals("太郎", result.rows[0][0])
        assertEquals("東京", result.rows[0][2])
    }

    @Test
    @DisplayName("Auto-detect comma delimiter")
    fun testDetectCommaDelimiter() {
        val content = "a,b,c\n1,2,3"
        
        val delimiter = parser.detectDelimiter(content, "data.csv")
        
        assertEquals(',', delimiter)
    }

    @Test
    @DisplayName("Auto-detect tab delimiter from TSV extension")
    fun testDetectTabFromExtension() {
        val content = "a\tb\tc\n1\t2\t3"
        
        val delimiter = parser.detectDelimiter(content, "data.tsv")
        
        assertEquals('\t', delimiter)
    }

    @Test
    @DisplayName("Auto-detect semicolon delimiter from content")
    fun testDetectSemicolonFromContent() {
        val content = "a;b;c\n1;2;3"
        
        val delimiter = parser.detectDelimiter(content, "data.dsv")
        
        assertEquals(';', delimiter)
    }

    @Test
    @DisplayName("Trim whitespace from unquoted fields")
    fun testWhitespaceTrimming() {
        val content = "  Name  ,  Age  \n  John  ,  30  "

        val result = parser.parse(content, ',', hasHeader = true)

        assertEquals("Name", result.headers[0])
        assertEquals("Age", result.headers[1])
        assertEquals("John", result.rows[0][0])
        assertEquals("30", result.rows[0][1])
    }
}
