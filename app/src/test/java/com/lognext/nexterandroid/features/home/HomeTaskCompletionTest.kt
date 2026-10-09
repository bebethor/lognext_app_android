package com.lognext.nexterandroid.features.home

import com.google.gson.JsonParser
import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class HomeTaskCompletionTest {
    @Test fun completionAndReopeningPersistAcrossServiceReloads() = runBlocking {
        val api = object : APIClient {
            var completed = false
            var deleted = false
            override fun execute(request: Request, acceptedStatusCodes: IntRange): String {
                return when (request.method) {
                    "PATCH" -> {
                        assertEquals("/api/v1/tasks/task-1", request.url.encodedPath)
                        val buffer = Buffer()
                        request.body!!.writeTo(buffer)
                        val json = JsonParser.parseString(buffer.readUtf8()).asJsonObject
                        assertTrue(json.has("is_completed"))
                        assertFalse(json.has("percent_complete"))
                        completed = json.get("is_completed").asBoolean
                        assertTrue(204 in acceptedStatusCodes)
                        ""
                    }
                    "DELETE" -> {
                        assertEquals("/api/v1/tasks/task-1", request.url.encodedPath)
                        assertTrue(204 in acceptedStatusCodes)
                        deleted = true
                        ""
                    }
                    "GET" -> {
                        assertEquals("/api/v1/tasks/me", request.url.encodedPath)
                        if (deleted) """{"tasks":[]}""" else
                            """{"tasks":[{"id":"task-1","title":"Test","is_completed":$completed}]}"""
                    }
                    else -> error("Unexpected request")
                }
            }
        }
        HomeService(api).updateTask("task-1", HomeTaskUpdateRequest(isCompleted = true))
        assertTrue(HomeService(api).listTasks().tasks.single().isDone)
        HomeService(api).updateTask("task-1", HomeTaskUpdateRequest(isCompleted = false))
        assertFalse(HomeService(api).listTasks().tasks.single().isDone)
        assertFalse(api.deleted)
        HomeService(api).deleteTask("task-1")
        assertTrue(HomeService(api).listTasks().tasks.isEmpty())
    }
}
