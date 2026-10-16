package com.lognext.nexterandroid.core.auth

import org.junit.Assert.assertNotNull
import org.junit.Test

class MsalRuntimeDependenciesTest {
    @Test fun interactiveLoginHasTheBroadcastDependencyRequiredByMsal() {
        // MSAL loads this from its background executor before opening Microsoft.
        // A missing runtime dependency otherwise leaves its callback unresolved.
        assertNotNull(Class.forName(
            "androidx.localbroadcastmanager.content.LocalBroadcastManager",
            false,
            javaClass.classLoader
        ))
    }
}
