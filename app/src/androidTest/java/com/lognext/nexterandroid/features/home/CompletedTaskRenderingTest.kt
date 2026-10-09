package com.lognext.nexterandroid.features.home

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.lognext.nexterandroid.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompletedTaskRenderingTest {
    @Test fun completedTaskRemainsVisibleWithItsCheckAfterReload() {
        val task = Gson().fromJson(
            """{"id":"test","title":"Tarea completada conservada","is_completed":true}""",
            HomeTask::class.java
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    TasksCard(
                        HomeUiState(tasks = listOf(task), completedTaskIds = setOf(task.stableId)),
                        onAddTask = {}, onToggleCompleted = {}, onDeleteTask = {}
                    )
                }
            }
            val deadline = System.currentTimeMillis() + 10000
            var visible = false
            while (!visible && System.currentTimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                val root = instrumentation.uiAutomation.rootInActiveWindow
                visible = contains(root, task.displayTitle) && contains(root, "✓")
                if (!visible) Thread.sleep(200)
            }
            assertTrue("La tarea completada y su check deben seguir visibles", visible)
        }
    }

    private fun contains(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.isVisibleToUser && node.text?.toString() == text) return true
        return (0 until node.childCount).any { contains(node.getChild(it), text) }
    }
}
