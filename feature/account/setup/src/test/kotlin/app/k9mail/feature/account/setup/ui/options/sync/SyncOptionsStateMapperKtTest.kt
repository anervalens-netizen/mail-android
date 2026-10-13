package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import org.junit.Test

class SyncOptionsStateMapperKtTest {

    @Test
    fun `new POP3 defaults to all messages while IMAP keeps its default`() {
        val incoming = ServerSettings(
            type = "pop3",
            host = "example.invalid",
            port = 995,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "test",
            password = "test",
            clientCertificateAlias = null,
        )
        val pop3 = AccountState(incomingServerSettings = incoming).toSyncOptionsState()
        val imap = AccountState(incomingServerSettings = incoming.copy(type = "imap")).toSyncOptionsState()

        assertThat(pop3.messageDisplayCount).isEqualTo(EmailDisplayCount.ALL_MESSAGES)
        assertThat(imap.messageDisplayCount).isEqualTo(EmailDisplayCount.DEFAULT)
    }

    @Test
    fun `unlimited choice survives the setup round trip`() {
        val state = SyncOptionsContract.State(messageDisplayCount = EmailDisplayCount.ALL_MESSAGES)

        val restored = AccountState(syncOptions = state.toAccountSyncOptions()).toSyncOptionsState()

        assertThat(restored.messageDisplayCount).isEqualTo(EmailDisplayCount.ALL_MESSAGES)
    }

    @Test
    fun `should map state to account options`() {
        val state = SyncOptionsContract.State(
            checkFrequency = EmailCheckFrequency.EVERY_2_HOURS,
            messageDisplayCount = EmailDisplayCount.MESSAGES_100,
            showNotification = true,
        )

        val result = state.toAccountSyncOptions()

        assertThat(result).isEqualTo(
            AccountSyncOptions(
                checkFrequencyInMinutes = 120,
                messageDisplayCount = 100,
                showNotification = true,
            ),
        )
    }
}
