package net.thunderbird.app.common.rules

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
internal data class MailSenderRule(val sender: String, val folderId: Long) {
    fun matches(addresses: List<String>): Boolean =
        addresses.any { normalizeSender(it) == sender }

    companion object {
        private const val MIN_SENDER_LENGTH = 3
        private const val MAX_SENDER_LENGTH = 254
        private const val MAX_FOLDER_LENGTH = 100

        fun normalizeSender(value: String): String = value.trim().lowercase(Locale.ROOT)

        fun isValidSender(value: String): Boolean =
            value.length in MIN_SENDER_LENGTH..MAX_SENDER_LENGTH && value.count { it == '@' } == 1 &&
                value.substringBefore('@').isNotEmpty() && value.substringAfter('@').contains('.') &&
                value.none { it.isWhitespace() || it in "<>,;\"()\\\\" || it.isISOControl() }

        fun isValidFolderName(value: String): Boolean =
            value.isNotBlank() && value.length <= MAX_FOLDER_LENGTH &&
                value.none { it.isISOControl() || it in "/\\\\" }
    }
}

internal fun List<MailSenderRule>.upsert(rule: MailSenderRule): List<MailSenderRule> =
    filterNot { it.sender == rule.sender } + rule
