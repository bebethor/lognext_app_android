package com.lognext.nexterandroid

import com.google.gson.JsonParser
import com.lognext.nexterandroid.core.auth.MsalSigningConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URI
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory

class MsalSigningConfigurationTest {
    @Test
    fun playConsoleCertificatesSelectTheCorrectConfiguration() {
        assertEquals(R.raw.msal_auth_config_play_original, MsalSigningConfiguration.resourceForHash(
            hashFromSha1("85:F2:E8:24:46:B2:53:FE:45:54:0E:BD:83:25:30:97:74:05:40:AC")
        ))
        assertEquals(R.raw.msal_auth_config_play_current, MsalSigningConfiguration.resourceForHash(
            hashFromSha1("9C:62:3C:D6:9C:AD:7E:CE:92:E8:50:C1:48:FF:08:07:4A:57:DA:80")
        ))
    }

    @Test
    fun developmentCertificateStillWorks() {
        assertEquals(R.raw.msal_auth_config, MsalSigningConfiguration.resourceForHash(
            hashFromSha1("DB:9E:0A:58:C5:8A:B4:3B:B1:1D:15:4C:20:42:52:4D:58:BF:B6:61")
        ))
    }

    @Test(expected = IllegalStateException::class)
    fun unknownSigningCertificateIsRejected() {
        MsalSigningConfiguration.resourceForHash("unregistered-signature")
    }

    @Test
    fun directlyInstalledApkUsesUploadCertificateConfiguration() {
        assertEquals(R.raw.msal_auth_config_upload, MsalSigningConfiguration.resourceForHash(
            hashFromSha1("05:A0:42:E4:29:BA:2A:81:8E:53:D3:65:15:67:BC:6E:B0:00:95:58")
        ))
    }

    @Test
    fun everyConfigurationHasAnExactManifestCallbackAndBrokerValidation() {
        val document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val activities = document.getElementsByTagName("activity")
        val android = "http://schemas.android.com/apk/res/android"
        val browser = (0 until activities.length).map { activities.item(it) as org.w3c.dom.Element }
            .single { it.getAttributeNS(android, "name") == "com.microsoft.identity.client.BrowserTabActivity" }
        val data = browser.getElementsByTagName("data")
        val callbacks = (0 until data.length).map {
            val element = data.item(it) as org.w3c.dom.Element
            Triple(element.getAttributeNS(android, "scheme"), element.getAttributeNS(android, "host"),
                element.getAttributeNS(android, "path"))
        }
        val configurations = mapOf(
            "msal_auth_config" to MsalSigningConfiguration.DEVELOPMENT_HASH,
            "msal_auth_config_play_original" to MsalSigningConfiguration.PLAY_ORIGINAL_HASH,
            "msal_auth_config_play_current" to MsalSigningConfiguration.PLAY_CURRENT_HASH,
            "msal_auth_config_upload" to MsalSigningConfiguration.UPLOAD_HASH
        )
        configurations.forEach { (name, hash) ->
            val json = JsonParser.parseString(File("src/main/res/raw/$name.json").readText()).asJsonObject
            val uri = URI(json["redirect_uri"].asString)
            assertEquals("/$hash", uri.path)
            assertTrue(callbacks.contains(Triple(uri.scheme, uri.host, uri.path)))
            assertTrue(json["broker_redirect_uri_registered"].asBoolean)
            assertEquals("02734a6d-c892-48c3-9388-b548ac422deb", json["client_id"].asString)
        }
    }

    private fun hashFromSha1(sha1: String): String = Base64.getEncoder().encodeToString(
        sha1.split(':').map { it.toInt(16).toByte() }.toByteArray()
    )
}
