package com.fsck.k9.backend.pop3

import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.store.pop3.Pop3Folder
import com.fsck.k9.mail.store.pop3.Pop3Message
import com.fsck.k9.mail.store.pop3.Pop3Store
import net.thunderbird.core.common.exception.MessagingException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class CommandDownloadMessageTest {
    private val message = mock<Pop3Message>()
    private val folder = mock<Pop3Folder> {
        on { getMessage("uid") } doReturn message
    }
    private val store = mock<Pop3Store> {
        on { getFolder("INBOX") } doReturn folder
    }
    private val backendFolder = mock<BackendFolder>()
    private val storage = mock<BackendStorage> {
        on { getFolder("INBOX") } doReturn backendFolder
    }
    private val testSubject = CommandDownloadMessage(storage, store)

    @Test
    fun `full retrieval saves complete message without deleting server copy`() {
        testSubject.downloadCompleteMessage("INBOX", "uid")

        verify(folder).fetch(any(), any(), anyOrNull(), eq(0))
        verify(backendFolder).saveMessage(message, MessageDownloadState.FULL)
        verify(folder, never()).setFlags(any(), any(), any())
        verify(folder).close()
    }

    @Test
    fun `failed retrieval does not replace local message or delete server copy`() {
        doThrow(MessagingException("network failure")).`when`(folder)
            .fetch(any(), any(), anyOrNull(), eq(0))

        assertThrows(MessagingException::class.java) { testSubject.downloadCompleteMessage("INBOX", "uid") }

        verify(backendFolder, never()).saveMessage(any(), any())
        verify(folder, never()).setFlags(any(), any(), any())
        verify(folder).close()
    }
}
