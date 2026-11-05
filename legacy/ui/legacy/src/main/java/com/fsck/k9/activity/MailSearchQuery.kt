package com.fsck.k9.activity

private val fts4WordPattern = Regex("[\\p{L}\\p{N}]+")

/**
 * FTS4 MATCH has its own query grammar. Tokenize user text and quote every term,
 * so quotes, parentheses, operators and field syntax cannot turn a mail search into an invalid query.
 * Multiple words retain the previous AND semantics; punctuation-only input skips body search.
 */
internal fun String.toSafeFts4MatchQuery(): String? {
    val words = fts4WordPattern.findAll(this).map { it.value }.toList()
    return words.takeIf { it.isNotEmpty() }?.joinToString(separator = " AND ") { "\"$it\"" }
}
