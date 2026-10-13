package net.thunderbird.app.common.rules

import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.controller.SimpleMessagingListener
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.ControllerExtension
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.notification.NotificationController
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.Logger

/**
 * POP3 has no server folders. All rules run on the controller queue, after its POP3 session closes.
 * Moving directly through MessageStore is deliberate: it preserves the source UIDL as a deleted
 * placeholder, without submitting a remote MOVE or DELE operation.
 */
internal class MailLocalRules(
    private val accounts: LegacyAccountDtoManager,
    private val stores: MessageStoreManager,
    private val localStores: LocalStoreProvider,
    private val notifications: NotificationController,
    private val logger: Logger,
) : ControllerExtension {
    private lateinit var queue: ControllerExtension.ControllerInternals
    private lateinit var controller: MessagingController
    private lateinit var backends: BackendManager
    private val json = Json { ignoreUnknownKeys = true }

    override fun init(
        controller: MessagingController,
        backendManager: BackendManager,
        controllerInternals: ControllerExtension.ControllerInternals,
    ) {
        this.controller = controller
        queue = controllerInternals
        backends = backendManager
        controller.addListener(object : SimpleMessagingListener() {
            override fun synchronizeMailboxFinished(account: LegacyAccountDto, folderId: Long) {
                if (account.incomingServerSettings.type != Protocols.POP3) return
                queue.putBackground("Apply local sender rules", null) {
                    try {
                        val current = accounts.getAccount(account.uuid) ?: return@putBackground
                        val store = stores.getMessageStore(current)
                        if (store.getFolderId(INBOX) == folderId) applyRules(current, store)
                    } catch (_: Exception) {
                        // Exceptions from mail backends may contain credentials or message data.
                        logger.warn { "Local sender rules could not finish; retrying at the next sync" }
                    }
                }
            }
        })
    }

    suspend fun load(accountUuid: String? = null): MailRulesSnapshot = queued {
        val available = accounts.getAccounts()
            .filter { it.isFinishedSetup && it.incomingServerSettings.type == Protocols.POP3 }
        val selected = available.firstOrNull { it.uuid == accountUuid } ?: available.firstOrNull()
        MailRulesSnapshot(
            accounts = available.map { MailRuleAccount(it.uuid, it.name ?: it.email) },
            selectedUuid = selected?.uuid,
            rules = selected?.let { ruleRows(stores.getMessageStore(it)) }.orEmpty(),
        )
    }

    suspend fun saveAndApply(accountUuid: String, senderInput: String, folderInput: String): MailRulesResult = queued {
        val sender = MailSenderRule.normalizeSender(senderInput)
        val folderName = folderInput.trim()
        require(MailSenderRule.isValidSender(sender))
        require(MailSenderRule.isValidFolderName(folderName))
        val account = requirePop3Account(accountUuid)
        val store = stores.getMessageStore(account)
        // Decode before creating anything; malformed saved data must never be silently overwritten.
        val rules = readRules(store)
        val existing = store.getFolders(excludeLocalOnly = false) {
            FolderTarget(it.id, it.name, it.isLocalOnly && it.type == FolderType.REGULAR)
        }.firstOrNull { it.name.equals(folderName, ignoreCase = true) }
        if (existing != null && !existing.allowed) throw MailRuleFolderConflict()
        val folderId =
            existing?.id ?: localStores.getInstance(account).createLocalFolder(folderName, FolderType.REGULAR)
        store.setSyncEnabled(folderId, false)
        store.setPushEnabled(folderId, false)
        store.setNotificationsEnabled(folderId, true)
        store.setVisible(folderId, true)
        store.setExtraString(RULES_KEY, json.encodeToString(rules.upsert(MailSenderRule(sender, folderId))))
        applyRules(account, store)
    }

    suspend fun remove(accountUuid: String, sender: String) = queued {
        val store = stores.getMessageStore(requirePop3Account(accountUuid))
        store.setExtraString(RULES_KEY, json.encodeToString(readRules(store).filterNot { it.sender == sender }))
    }

    private fun requirePop3Account(uuid: String): LegacyAccountDto {
        val account = requireNotNull(accounts.getAccount(uuid))
        require(account.incomingServerSettings.type == Protocols.POP3)
        return account
    }

    private fun readRules(store: MessageStore): List<MailSenderRule> =
        store.getExtraString(RULES_KEY)?.let { json.decodeFromString<List<MailSenderRule>>(it) }.orEmpty()

    private fun ruleRows(store: MessageStore): List<MailRuleRow> = readRules(store).map { rule ->
        MailRuleRow(rule.sender, store.getFolder(rule.folderId) { it.name })
    }

    private fun applyRules(account: LegacyAccountDto, store: MessageStore): MailRulesResult {
        val rules = readRules(store)
        val inboxId = store.getFolderId(INBOX)
        if (rules.isEmpty() || inboxId == null) return MailRulesResult()
        val candidates = store.getMessages(
            selection = "messages.folder_id = ?",
            selectionArgs = arrayOf(inboxId.toString()),
            sortOrder = "messages.id ASC",
        ) { message ->
            rules.firstOrNull { it.matches(message.fromAddresses.map { address -> address.address }) }
                ?.let { Candidate(message.messageServerId, it) }
        }
        var moved = 0
        var pending = 0
        for (candidate in candidates) {
            try {
                if (moveCompleteMessage(account, store, inboxId, candidate)) moved++ else pending++
            } catch (_: Exception) {
                pending++
                logger.warn { "A local rule left a message in Inbox; retrying at the next sync" }
            }
        }
        if (moved > 0) {
            val changedFolders = rules.map { it.folderId }.toSet() + inboxId
            for (listener in controller.listeners) {
                for (folderId in changedFolders) listener.folderStatusChanged(account, folderId)
            }
        }
        return MailRulesResult(moved, pending)
    }

    private fun moveCompleteMessage(
        account: LegacyAccountDto,
        store: MessageStore,
        inboxId: Long,
        candidate: Candidate,
    ): Boolean {
        val destinationValid = store.getFolder(candidate.rule.folderId) {
            it.isLocalOnly && it.type == FolderType.REGULAR
        } == true
        if (!destinationValid) return false

        return completeMessageId(account, store, inboxId, candidate)?.let { messageId ->
            store.moveMessage(messageId, candidate.rule.folderId)
            // A notification pointing to the old Inbox UID would no longer open the message.
            notifications.removeNewMailNotification(account, MessageReference(account.uuid, inboxId, candidate.uid))
            true
        } ?: false
    }

    private fun completeMessageId(
        account: LegacyAccountDto,
        store: MessageStore,
        inboxId: Long,
        candidate: Candidate,
    ): Long? {
        if (Flag.X_DOWNLOADED_FULL !in store.getMessageFlags(inboxId, candidate.uid)) {
            backends.getBackend(account.uuid).downloadCompleteMessage(INBOX, candidate.uid)
        }
        if (Flag.X_DOWNLOADED_FULL !in store.getMessageFlags(inboxId, candidate.uid)) return null

        // Full download can replace the local row. Resolve its current ID, and recheck the sender.
        return store.getMessages(
            selection = "messages.folder_id = ? AND messages.uid = ?",
            selectionArgs = arrayOf(inboxId.toString(), candidate.uid),
            sortOrder = "messages.id ASC",
        ) {
            if (candidate.rule.matches(it.fromAddresses.map { address -> address.address })) it.id else null
        }.singleOrNull()
    }

    private suspend fun <T> queued(operation: () -> T): T = suspendCancellableCoroutine { continuation ->
        queue.putBackground("Manage local sender rules", null) {
            if (continuation.isActive) {
                val result = runCatching(operation)
                if (continuation.isActive) continuation.resumeWith(result)
            }
        }
    }

    private data class Candidate(val uid: String, val rule: MailSenderRule)
    private data class FolderTarget(val id: Long, val name: String, val allowed: Boolean)

    companion object {
        private const val RULES_KEY = "mail.localSenderRules.v1"
        private const val INBOX = "INBOX"
    }
}

internal class MailRuleFolderConflict : IllegalArgumentException()
internal data class MailRuleAccount(val uuid: String, val label: String)
internal data class MailRuleRow(val sender: String, val folderName: String?)
internal data class MailRulesSnapshot(
    val accounts: List<MailRuleAccount>,
    val selectedUuid: String?,
    val rules: List<MailRuleRow>,
)
internal data class MailRulesResult(val moved: Int = 0, val pending: Int = 0)
