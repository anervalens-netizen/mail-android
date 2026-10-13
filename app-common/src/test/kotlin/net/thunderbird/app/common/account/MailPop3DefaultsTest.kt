package net.thunderbird.app.common.account

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import org.junit.Test

class MailPop3DefaultsTest {
    @Test
    fun `POP3 keeps local mail after server removal without a message count limit`() {
        val testSubject = newAccount()

        testSubject.applyMailPop3Defaults(Protocols.POP3)

        assertThat(testSubject.isSyncRemoteDeletions).isFalse()
        assertThat(testSubject.displayCount).isEqualTo(0)
    }

    @Test
    fun `IMAP retains its synchronization and selected message count`() {
        val testSubject = newAccount()

        testSubject.applyMailPop3Defaults(Protocols.IMAP)

        assertThat(testSubject.isSyncRemoteDeletions).isTrue()
        assertThat(testSubject.displayCount).isEqualTo(50)
    }

    private fun newAccount() = LegacyAccountDto(
        uuid = "e4b7490c-4732-4a36-9f46-dbe68e4bd776",
    ).apply {
        isSyncRemoteDeletions = true
        displayCount = 50
    }
}
