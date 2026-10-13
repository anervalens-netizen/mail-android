package net.thunderbird.app.common.account

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols

// Mail fork: incoming POP3 mail is an independent local archive.
internal fun LegacyAccountDto.applyMailPop3Defaults(protocol: String) {
    if (protocol == Protocols.POP3) {
        isSyncRemoteDeletions = false
        displayCount = 0
    }
}
