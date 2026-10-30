package com.automattic.simplenote

import org.junit.Assert.*
import org.junit.Test

class NoteMarkdownFragmentTest {
    @Test
    fun `crlf and lf notes render identically`() {
        val lf = "```\ncode\n```\n\ntext after"
        val crlf = lf.replace("\n", "\r\n")
        assertEquals(
            NoteMarkdownFragment.getMarkdownFormattedContent("", lf),
            NoteMarkdownFragment.getMarkdownFormattedContent("", crlf)
        )
    }
}
