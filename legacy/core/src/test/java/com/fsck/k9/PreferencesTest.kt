package com.fsck.k9

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_OTHER_RAW
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID_RAW
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.preferences.TestStoragePersister
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler
import net.thunderbird.feature.account.storage.legacy.LegacyAvatarDtoStorageHandler
import net.thunderbird.feature.account.storage.legacy.LegacyProfileDtoStorageHandler
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PreferencesTest {

    private val logger: Logger = TestLogger()

    private val preferences = Preferences(
        storagePersister = TestStoragePersister(
            logger = logger,
        ),
        localStoreProvider = mock(),
        legacyAccountStorageHandler = LegacyAccountStorageHandler(
            serverSettingsDtoSerializer = mock {
                on { serialize(any()) } doReturn ""
                on { deserialize(any()) } doReturn SERVER_SETTINGS
            },
            profileDtoStorageHandler = LegacyProfileDtoStorageHandler(
                avatarDtoStorageHandler = LegacyAvatarDtoStorageHandler(),
            ),
            logger,
        ),
        accountDefaultsProvider = mock(),
    )

    @Before
    fun setUp() {
        // Currently necessary for initialization
        preferences.loadAccounts()
    }

    @Test
    fun `reloading accounts should return same Account instance`() {
        createAndSaveAccount(ACCOUNT_ID_RAW)
        createAndSaveAccount(ACCOUNT_ID_OTHER_RAW)
        val firstAccountOne = preferences.getAccount(ACCOUNT_ID_RAW)

        preferences.loadAccounts()

        val firstAccountTwo = preferences.getAccount(ACCOUNT_ID_RAW)
        assertThat(firstAccountTwo).isSameInstanceAs(firstAccountOne)
    }

    @Test
    fun `saving accounts should return updated Account instance`() {
        val account = createAccount(ACCOUNT_ID_RAW)
        preferences.saveAccount(account)

        val updatedAccount = createAccount(ACCOUNT_ID_RAW).apply {
            name = "New name"
        }

        preferences.saveAccount(updatedAccount)

        val currentAccountOne = preferences.getAccount(ACCOUNT_ID_RAW)!!
        assertThat(currentAccountOne.name).isEqualTo("New name")
    }

    @Test
    fun `small limits are migrated once while unlimited and later choices are preserved`() {
        createAndSaveAccount(ACCOUNT_ID_RAW)
        createAndSaveAccount(ACCOUNT_ID_OTHER_RAW)
        val oldKey = "$ACCOUNT_ID_RAW.maximumAutoDownloadMessageSize"
        val customKey = "$ACCOUNT_ID_OTHER_RAW.maximumAutoDownloadMessageSize"
        preferences.createStorageEditor()
            .remove("mail.autoDownloadLimit10MiBMigrated")
            .putInt(oldKey, 131072)
            .putInt(customKey, 5242880)
            .commit()

        preferences.loadAccounts()

        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize).isEqualTo(10485760)
        assertThat(preferences.getAccount(ACCOUNT_ID_OTHER_RAW)!!.maximumAutoDownloadMessageSize).isEqualTo(10485760)
        preferences.createStorageEditor()
            .remove("mail.autoDownloadLimit10MiBMigrated")
            .putInt(oldKey, 0)
            .putInt(customKey, 20971520)
            .commit()
        preferences.loadAccounts()
        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize).isEqualTo(0)
        assertThat(preferences.getAccount(ACCOUNT_ID_OTHER_RAW)!!.maximumAutoDownloadMessageSize).isEqualTo(20971520)

        preferences.createStorageEditor().putInt(oldKey, 131072).commit()
        preferences.loadAccounts()
        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize).isEqualTo(131072)
    }

    private fun createAccount(accountId: String): LegacyAccountDto {
        return LegacyAccountDto(
            uuid = accountId,
            isSensitiveDebugLoggingEnabled = { false },
        ).apply {
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
        }
    }

    private fun createAndSaveAccount(accountUuid: String) {
        val account = preferences.newAccount(accountUuid).apply {
            // To be able to persist `Account` we need to set server settings
            incomingServerSettings = SERVER_SETTINGS
            outgoingServerSettings = SERVER_SETTINGS
        }

        preferences.saveAccount(account)
    }

    companion object {
        private val SERVER_SETTINGS = ServerSettings(
            type = "irrelevant",
            host = "irrelevant",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "username",
            password = null,
            clientCertificateAlias = null,
        )
    }
}
