package com.lognext.nexterandroid.features.home

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lognext.nexterandroid.MainActivity
import com.lognext.nexterandroid.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MeetingEditingRenderingTest {
    @Test fun editorPrefillsMeetingAndDisablesSaveUntilSomethingChanges() {
        val meeting = HomeCalendarEvent(
            "preview", "Planificación del equipo", "2026-10-26T13:00:00Z", "2026-10-26T14:00:00Z",
            "Sala de prueba", true, "https://teams.microsoft.com/preview", 2, "Organizer",
            listOf(MeetingAttendee("Ana Ejemplo", "ana@example.com"), MeetingAttendee("Luis Ejemplo", "luis@example.com")),
            RoomReservation("sala@example.com", "Sala de prueba", "accepted"), true, "occurrence", "preview-version"
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                EditMeetingDialog(meeting, onDismiss = {}, onSaved = { fail("Unchanged meeting must not save") })
            } }
            var title: AccessibilityNodeInfo? = null
            val deadline = System.currentTimeMillis() + 10000
            while (title == null && System.currentTimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                title = find(instrumentation.uiAutomation.rootInActiveWindow, meeting.subject)
                if (title == null) Thread.sleep(100)
            }
            assertNotNull("The editor must prefill the original title", title)
            instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                File(context.filesDir, "meeting-edit-preview.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
            val saveTitle = context.getString(R.string.parity_save)
            var save: AccessibilityNodeInfo? = null
            repeat(8) {
                instrumentation.waitForIdleSync()
                save = find(instrumentation.uiAutomation.rootInActiveWindow, saveTitle)
                if (save == null) {
                    val metrics = context.resources.displayMetrics
                    val x = metrics.widthPixels / 2
                    val from = metrics.heightPixels * 3 / 4
                    val to = metrics.heightPixels / 4
                    instrumentation.uiAutomation.executeShellCommand("input swipe $x $from $x $to 250").close()
                    Thread.sleep(400)
                }
            }
            assertNotNull("The editor must expose its Save action", save)
            val saveNode = save!!
            assertFalse("Saving an unchanged event must be disabled", saveNode.isEnabled && (saveNode.parent?.isEnabled != false))
        }
    }

    private fun find(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isVisibleToUser && node.text?.toString() == text) return node
        return (0 until node.childCount).firstNotNullOfOrNull { find(node.getChild(it), text) }
    }

}
