package com.automattic.simplenote

import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTableTest {

    @Test
    fun testMarkdownTableEol() {
        val sourceTables = arrayOf(
            "| A | B |\r| --- | --- |\r| 1 | 2 |\r| 3 | 4 |",
            "| A | B |\r\n| --- | --- |\r\n| 1 | 2 |\r\n| 3 | 4 |",
            "| A | B |\n| --- | --- |\n| 1 | 2 |\n| 3 | 4 |"
        )

        for (sourceTable in sourceTables) {
            val table = NoteMarkdownFragment.getMarkdownFormattedContent("", sourceTable)
            assertTrue(table, table.contains("<table"))
        }
    }
}
