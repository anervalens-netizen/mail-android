package com.fsck.k9.activity

import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.search.legacy.api.SearchCondition
import net.thunderbird.feature.search.legacy.sql.SqlWhereClause
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MailSearchFtsSqliteTest {
    @Test
    fun `unbalanced parenthesis must not hide a subject match`() {
        withSearchDatabase { database ->
            database.execSQL("INSERT INTO messages(id, subject) VALUES (1, 'Invoice (2026')")
            database.execSQL("INSERT INTO messages_fulltext(docid, fulltext) VALUES (1, 'unrelated body')")

            val query = "Invoice (2026"
            val search = LocalMessageSearch().apply {
                or(SearchCondition(MessageSearchField.SUBJECT, SearchAttribute.CONTAINS, query))
                or(
                    SearchCondition(
                        MessageSearchField.MESSAGE_CONTENTS,
                        SearchAttribute.CONTAINS,
                        requireNotNull(query.toSafeFts4MatchQuery()),
                    ),
                )
            }
            val clause = SqlWhereClause.Builder().withConditions(search.conditions).build()

            database.rawQuery(
                "SELECT messages.id FROM messages WHERE ${clause.selection}",
                clause.selectionArgs.toTypedArray(),
            ).use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
            }
        }
    }

    @Test
    fun `unbalanced quote searches the downloaded message body`() {
        withSearchDatabase { database ->
            database.execSQL("INSERT INTO messages(id, subject) VALUES (2, 'Other subject')")
            database.execSQL("INSERT INTO messages_fulltext(docid, fulltext) VALUES (2, 'RCA document')")

            database.rawQuery(
                "SELECT docid FROM messages_fulltext WHERE fulltext MATCH ?",
                arrayOf(requireNotNull("\"RCA".toSafeFts4MatchQuery())),
            ).use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
            }
        }
    }

    private fun withSearchDatabase(block: (SQLiteDatabase) -> Unit) {
        val database = SQLiteDatabase.create(null)
        try {
            database.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, subject TEXT)")
            database.execSQL("CREATE VIRTUAL TABLE messages_fulltext USING fts4 (fulltext)")
            block(database)
        } finally {
            database.close()
        }
    }
}
