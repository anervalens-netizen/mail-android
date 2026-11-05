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
    fun `existing POP3 account at legacy download default is migrated and persisted`() {
        val account = createAccount(ACCOUNT_ID_RAW).apply {
            maximumAutoDownloadMessageSize = 128 * 1024
            markSetupFinished()
        }

        preferences.saveAccount(account)
        // Simulate a setting stored by Mail 1.2, before migration markers existed.
        preferences.createStorageEditor().apply {
            remove(account.uuid + MAIL_POP3_DOWNLOAD_LIMIT_MIGRATION_KEY_SUFFIX)
            commit()
        }

        preferences.loadAccounts()
        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize)
            .isEqualTo(10 * 1024 * 1024)
        val migrationKey = account.uuid + MAIL_POP3_DOWNLOAD_LIMIT_MIGRATION_KEY_SUFFIX
        assertThat(preferences.storage.getBoolean(migrationKey, false)).isEqualTo(true)

        // A manual choice made AFTER migration must survive saveAccount() and repeated reloads.
        val migratedAccount = preferences.getAccount(ACCOUNT_ID_RAW)!!
        migratedAccount.maximumAutoDownloadMessageSize = 128 * 1024
        preferences.saveAccount(migratedAccount)
        preferences.loadAccounts()
        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize)
            .isEqualTo(128 * 1024)
    }

    @Test
    fun `128 KiB explicitly selected on a new POP3 account is never upgraded`() {
        val account = createAccount(ACCOUNT_ID_RAW).apply {
            maximumAutoDownloadMessageSize = 128 * 1024
            markSetupFinished()
        }

        preferences.saveAccount(account)
        preferences.loadAccounts()

        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize)
            .isEqualTo(128 * 1024)
    }

    @Test
    fun `existing POP3 account with custom download limit is preserved`() {
        val account = createAccount(ACCOUNT_ID_RAW).apply {
            maximumAutoDownloadMessageSize = 5 * 1024 * 1024
            markSetupFinished()
        }

        preferences.saveAccount(account)

        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize)
            .isEqualTo(5 * 1024 * 1024)

        // Legacy accounts with custom limits also get a marker, so choosing 128 KiB later sticks.
        preferences.createStorageEditor().apply {
            remove(account.uuid + MAIL_POP3_DOWNLOAD_LIMIT_MIGRATION_KEY_SUFFIX)
            commit()
        }
        preferences.loadAccounts()
        val updated = preferences.getAccount(ACCOUNT_ID_RAW)!!
        updated.maximumAutoDownloadMessageSize = 128 * 1024
        preferences.saveAccount(updated)
        preferences.loadAccounts()
        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize)
            .isEqualTo(128 * 1024)
    }

    @Test
    fun `incomplete POP3 account is not migrated`() {
        val account = createAccount(ACCOUNT_ID_RAW).apply {
            maximumAutoDownloadMessageSize = 128 * 1024
        }

        preferences.saveAccount(account)

        assertThat(preferences.getAccount(ACCOUNT_ID_RAW)!!.maximumAutoDownloadMessageSize)
            .isEqualTo(128 * 1024)
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
            type = "pop3",
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
