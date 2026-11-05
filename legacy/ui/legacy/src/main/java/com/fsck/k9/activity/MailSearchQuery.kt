package com.fsck.k9.activity

/**
 * The messages_fulltext table uses FTS4's default "simple" tokenizer. Its token characters
 * are ASCII letters/digits and every non-ASCII byte, including Unicode punctuation, combining
 * marks and emoji. Splitting on Unicode categories would break matches such as "invoice–2026".
 *
 * Separate tokens only at ASCII non-alphanumeric characters, then quote each MATCH term so
 * unmatched parentheses, quotes, operators and field selectors cannot become FTS syntax.
 */
internal fun String.toSafeFts4MatchQuery(): String? {
    val tokens = mutableListOf<String>()
    var tokenStart = -1

    for (index in indices) {
        val character = this[index]
        val isTokenCharacter = character in 'a'..'z' ||
            character in 'A'..'Z' ||
            character in '0'..'9' ||
            character.code >= 0x80

        if (isTokenCharacter && tokenStart == -1) {
            tokenStart = index
        } else if (!isTokenCharacter && tokenStart != -1) {
            tokens.add(substring(tokenStart, index))
            tokenStart = -1
        }
    }

    if (tokenStart != -1) tokens.add(substring(tokenStart))
    return tokens.takeIf { it.isNotEmpty() }?.joinToString(separator = " AND ") { "\"$it\"" }
}
