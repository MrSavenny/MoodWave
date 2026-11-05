package com.example.moodwave

import com.example.moodwave.sync.*
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.*
import org.junit.Test

class CloudClientTest {
    private fun useServer(block: (MockWebServer, CloudClient) -> Unit) {
        MockWebServer().use { server ->
            server.start()
            block(server,CloudClient(CloudConfig("test",server.url("/backup/").toString(),"test-user","test-token","test-password")))
        }
    }
    @Test fun uploadIsConditionalAndRestorable() = useServer { server, client ->
        val payload = com.example.moodwave.data.EncryptedBackup.encrypt("diario privado".toByteArray(),"test-password".toCharArray())
        server.enqueue(MockResponse().setResponseCode(201))
        client.upload("MoodWave_test.mwbackup",payload)
        val request = server.takeRequest()
        assertEquals("PUT",request.method)
        assertEquals("*",request.getHeader("If-None-Match"))
        assertArrayEquals(payload,request.body.readByteArray())
        server.enqueue(MockResponse().setBody(okio.Buffer().write(payload)))
        assertArrayEquals(payload,client.download("MoodWave_test.mwbackup"))
    }
    @Test fun duplicateRetryAcceptsOnlyIdenticalBytes() = useServer { server, client ->
        server.enqueue(MockResponse().setResponseCode(412)); server.enqueue(MockResponse().setBody("same"))
        client.upload("MoodWave_test.mwbackup","same".toByteArray())
        server.enqueue(MockResponse().setResponseCode(412)); server.enqueue(MockResponse().setBody("different"))
        assertTrue(runCatching { client.upload("MoodWave_test.mwbackup","same".toByteArray()) }.isFailure)
    }
    @Test fun listFiltersFilesAndRejectsEntities() = useServer { server, client ->
        server.enqueue(MockResponse().setResponseCode(207).setBody("""<d:multistatus xmlns:d="DAV:"><d:response><d:href>/backup/MoodWave_test.mwbackup</d:href></d:response><d:response><d:href>/backup/private.txt</d:href></d:response></d:multistatus>"""))
        assertEquals(listOf("MoodWave_test.mwbackup"),client.list())
        server.enqueue(MockResponse().setResponseCode(207).setBody("<!DOCTYPE x SYSTEM 'file:///test'><x/>"))
        assertTrue(runCatching { client.list() }.isFailure)
    }
    @Test fun redirectIsNotFollowed() = useServer { server, client ->
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location",server.url("/other")))
        assertTrue(runCatching { client.download("MoodWave_test.mwbackup") }.exceptionOrNull() is CloudHttpException)
        assertEquals(1,server.requestCount)
    }
    @Test fun validatesProductionEndpointAndRetryPolicy() {
        assertEquals("https://example.com/folder/",CloudClient.validateUrl("https://example.com/folder"))
        listOf("http://example.com", "https://user:pass@example.com", "https://example.com/?token=x").forEach {
            assertTrue(runCatching { CloudClient.validateUrl(it) }.isFailure)
        }
        assertTrue(CloudClient.retryable(503)); assertTrue(CloudClient.retryable(429))
        assertFalse(CloudClient.retryable(401)); assertFalse(CloudClient.retryable(403))
    }
}
