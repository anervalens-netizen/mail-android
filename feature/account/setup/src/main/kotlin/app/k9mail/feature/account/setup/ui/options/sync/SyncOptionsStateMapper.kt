package app.k9mail.feature.account.setup.ui.options.sync

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.setup.domain.entity.EmailCheckFrequency
import app.k9mail.feature.account.setup.domain.entity.EmailDisplayCount
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsContract.State
import net.thunderbird.core.common.mail.Protocols

internal fun AccountState.toSyncOptionsState(): State {
    val options = syncOptions
    return if (options == null) {
        State(
            messageDisplayCount = if (incomingServerSettings?.type == Protocols.POP3) {
                EmailDisplayCount.ALL_MESSAGES
            } else {
                EmailDisplayCount.DEFAULT
            },
        )
    } else {
        State(
            checkFrequency = EmailCheckFrequency.fromMinutes(options.checkFrequencyInMinutes),
            messageDisplayCount = EmailDisplayCount.fromCount(options.messageDisplayCount),
            showNotification = options.showNotification,
        )
    }
}

internal fun State.toAccountSyncOptions(): AccountSyncOptions {
    return AccountSyncOptions(
        checkFrequencyInMinutes = checkFrequency.minutes,
        messageDisplayCount = messageDisplayCount.count,
        showNotification = showNotification,
    )
}
