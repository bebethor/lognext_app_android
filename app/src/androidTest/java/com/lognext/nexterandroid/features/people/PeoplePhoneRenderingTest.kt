package com.lognext.nexterandroid.features.people

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lognext.nexterandroid.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeoplePhoneRenderingTest {
    @Test fun corporatePhoneIsRenderedInTheRealProfileDialog() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val number = "+34 000 000 000"
        val person = PersonProfileResponse(
            personCode = "ui-test",
            fullName = "Perfil de prueba",
            workPhone = number,
            email = "prueba@example.com"
        ).toPeopleRow()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    PersonDetailDialog(
                        person = person, manager = null, reports = emptyList(),
                        isLoading = false, errorMessage = null,
                        onPersonSelected = {}, onDismiss = {}
                    )
                }
            }
            val deadline = System.currentTimeMillis() + 10000
            var visible = false
            while (!visible && System.currentTimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                visible = containsVisibleText(instrumentation.uiAutomation.rootInActiveWindow, number)
                if (!visible) Thread.sleep(200)
            }
            assertTrue("El teléfono corporativo debe aparecer visible en el diálogo real", visible)
        }
    }

    private fun containsVisibleText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.isVisibleToUser && node.text?.toString() == text) return true
        return (0 until node.childCount).any { containsVisibleText(node.getChild(it), text) }
    }
}
