package com.fsck.k9.activity

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

class MailSearchQueryTest {
    @Test
    fun `FTS query treats unbalanced parentheses as literal text`() {
        assertThat("Invoice (2026".toSafeFts4MatchQuery()).isEqualTo("\"Invoice\" AND \"2026\"")
    }

    @Test
    fun `FTS query treats unbalanced quotes as literal text`() {
        assertThat("\"RCA".toSafeFts4MatchQuery()).isEqualTo("\"RCA\"")
    }

    @Test
    fun `FTS query does not interpret operators or field prefixes`() {
        assertThat("RCA OR subject:test".toSafeFts4MatchQuery())
            .isEqualTo("\"RCA\" AND \"OR\" AND \"subject\" AND \"test\"")
    }

    @Test
    fun `FTS query accepts Unicode tokens`() {
        assertThat("factură (2026".toSafeFts4MatchQuery()).isEqualTo("\"factură\" AND \"2026\"")
    }

    @Test
    fun `FTS query skips a termless punctuation-only body search`() {
        assertThat("(() \"".toSafeFts4MatchQuery()).isNull()
    }
}
