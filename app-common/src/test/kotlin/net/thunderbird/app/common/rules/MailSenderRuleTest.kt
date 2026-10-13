package net.thunderbird.app.common.rules

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class MailSenderRuleTest {
    @Test
    fun `matches exact address regardless of case and surrounding whitespace`() {
        val testSubject = MailSenderRule(MailSenderRule.normalizeSender("  Reports@Example.org "), 7)
        assertThat(testSubject.matches(listOf("REPORTS@example.org"))).isTrue()
        assertThat(testSubject.matches(listOf("else@example.org", " reports@example.org "))).isTrue()
    }

    @Test
    fun `does not match address prefixes domains or display names`() {
        val testSubject = MailSenderRule("reports@example.org", 7)
        for (address in listOf("otherreports@example.org", "reports@example.org.evil", "Reports <other@example.org>")) {
            assertThat(testSubject.matches(listOf(address))).isFalse()
        }
    }

    @Test
    fun `rejects malformed sender and unsafe folder input`() {
        val testSubject = MailSenderRule
        for (address in listOf("", "@example.org", "a@@example.org", "A <a@example.org>", "a\n@example.org")) {
            assertThat(testSubject.isValidSender(address)).isFalse()
        }
        for (folder in listOf("", "   ", "../Inbox", "bad\nfolder", "x".repeat(101))) {
            assertThat(testSubject.isValidFolderName(folder)).isFalse()
        }
        assertThat(testSubject.isValidFolderName("Reports 2026")).isTrue()
    }

    @Test
    fun `saving same sender updates destination without creating competing rules`() {
        val testSubject = listOf(MailSenderRule("reports@example.org", 7), MailSenderRule("other@example.org", 8))
        val result = testSubject.upsert(MailSenderRule("reports@example.org", 9))
        assertThat(result).isEqualTo(
            listOf(MailSenderRule("other@example.org", 8), MailSenderRule("reports@example.org", 9)),
        )
    }

    @Test
    fun `rules round trip with destination IDs`() {
        val testSubject = listOf(MailSenderRule("reports@example.org", 1234567890123))
        assertThat(Json.decodeFromString<List<MailSenderRule>>(Json.encodeToString(testSubject))).isEqualTo(testSubject)
    }
}
