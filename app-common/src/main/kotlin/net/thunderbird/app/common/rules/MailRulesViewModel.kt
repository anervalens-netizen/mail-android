package net.thunderbird.app.common.rules

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel

internal data class MailRulesState(
    val snapshot: MailRulesSnapshot = MailRulesSnapshot(emptyList(), null, emptyList()),
    val sender: String = "",
    val folder: String = "",
    val busy: Boolean = true,
    val error: MailRulesError? = null,
    val result: MailRulesResult? = null,
    val removed: Boolean = false,
)

internal enum class MailRulesError { INPUT, FOLDER_CONFLICT, OPERATION }

internal sealed interface MailRulesEvent {
    data class SelectAccount(val uuid: String) : MailRulesEvent
    data class SenderChanged(val value: String) : MailRulesEvent
    data class FolderChanged(val value: String) : MailRulesEvent
    data class Remove(val sender: String) : MailRulesEvent
    data object Save : MailRulesEvent
}

internal class MailRulesViewModel(private val rules: MailLocalRules) :
    BaseViewModel<MailRulesState, MailRulesEvent, Nothing>(MailRulesState()) {

    init {
        runOperation { refresh() }
    }

    override fun event(event: MailRulesEvent) {
        if (state.value.busy) return
        when (event) {
            is MailRulesEvent.SenderChanged -> updateState { it.copy(sender = event.value, error = null) }

            is MailRulesEvent.FolderChanged -> updateState { it.copy(folder = event.value, error = null) }

            is MailRulesEvent.SelectAccount -> runOperation {
                refresh(event.uuid)
                updateState { it.copy(sender = "", folder = "") }
            }

            is MailRulesEvent.Remove -> runOperation {
                rules.remove(requireNotNull(state.value.snapshot.selectedUuid), event.sender)
                refresh()
                updateState { it.copy(removed = true) }
            }

            MailRulesEvent.Save -> save()
        }
    }

    private fun save() {
        val current = state.value
        if (!MailSenderRule.isValidSender(MailSenderRule.normalizeSender(current.sender)) ||
            !MailSenderRule.isValidFolderName(current.folder.trim())
        ) {
            updateState { it.copy(error = MailRulesError.INPUT) }
            return
        }
        runOperation {
            val result = rules.saveAndApply(
                requireNotNull(current.snapshot.selectedUuid),
                current.sender,
                current.folder,
            )
            refresh()
            updateState { it.copy(result = result) }
        }
    }

    private suspend fun refresh(uuid: String? = state.value.snapshot.selectedUuid) {
        val snapshot = rules.load(uuid)
        updateState { it.copy(snapshot = snapshot) }
    }

    private fun runOperation(operation: suspend () -> Unit) {
        updateState { it.copy(busy = true, error = null, result = null, removed = false) }
        viewModelScope.launch {
            try {
                operation()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: MailRuleFolderConflict) {
                updateState { it.copy(error = MailRulesError.FOLDER_CONFLICT) }
            } catch (_: Exception) {
                updateState { it.copy(error = MailRulesError.OPERATION) }
            } finally {
                updateState { it.copy(busy = false) }
            }
        }
    }
}
