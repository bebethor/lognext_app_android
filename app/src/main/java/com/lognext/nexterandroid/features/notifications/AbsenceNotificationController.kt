package com.lognext.nexterandroid.features.notifications

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.lognext.nexterandroid.core.AppConfig
import com.lognext.nexterandroid.core.auth.AuthRepository
import com.lognext.nexterandroid.core.auth.AuthState
import com.lognext.nexterandroid.core.network.APIClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val ABSENCE_PREFS = "absence_push"
internal const val ABSENCE_CHANNEL = "absence_status"
internal const val ABSENCE_TAG = "AbsencePush"

data class AbsenceStatus(val available: Boolean = false, val enabled: Boolean = false, val subscription_id: String? = null)

class AbsenceNotificationController(private val context: Context, private val api: APIClient, private val auth: AuthRepository) {
    private val prefs = context.getSharedPreferences(ABSENCE_PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val gson = Gson()
    var status by mutableStateOf(AbsenceStatus()); private set
    var busy by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set
    private var renewedKey: String? = null
    private var renewedAt = 0L
    val pendingOpen = MutableStateFlow<String?>(null)
    val refreshGeneration = MutableStateFlow(0)
    private fun account() = (auth.authState.value as? AuthState.Authenticated)?.user?.username?.lowercase()
    private fun installation(): String = prefs.getString("installation", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("installation", it).commit()
    }
    private suspend fun request(method: String, installation: String, token: String? = null): AbsenceStatus = withContext(Dispatchers.IO) {
        val base = AppConfig.BaseUrl.trimEnd('/') + "/api/v1/me/absence-notifications"
        val url = when (method) { "GET" -> "$base?installation_id=$installation&platform=android"; "DELETE" -> "$base/$installation"; else -> base }
        val builder = Request.Builder().url(url).header("Cache-Control", "no-store")
        when (method) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
            else -> builder.put(gson.toJson(mapOf("installation_id" to installation, "device_token" to token, "platform" to "android", "message_version" to 2)).toRequestBody("application/json".toMediaType()))
        }
        var body = ""
        for (attempt in 0..2) {
            currentCoroutineContext().ensureActive()
            try { body = api.execute(builder.build()); break }
            catch (e: IOException) { if (attempt == 2) throw e; delay(500L * (attempt + 1)) }
        }
        Log.i(ABSENCE_TAG, "backend method=$method success=true")
        if (method == "DELETE") AbsenceStatus() else gson.fromJson(body, AbsenceStatus::class.java)
    }
    private suspend fun token(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (continuation.isActive) {
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(IOException("FCM registration failed"))
            }
        }
    }
    fun refresh() = perform {
        val owner = account() ?: return@perform
        if (prefs.getString("pending_owner", null) == owner) {
            val previous = prefs.getString("pending_installation", null)
            if (previous != null) request("DELETE", previous)
            prefs.edit().remove("pending_owner").remove("pending_installation").commit()
        }
        status = request("GET", installation())
        if (prefs.getString("owner", null) != owner) {
            prefs.edit().putString("owner", owner).putBoolean("opted_in", false).remove("subscription").commit()
        }
        if (status.available && prefs.getBoolean("opted_in", false)) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                request("DELETE", installation())
                prefs.edit().putBoolean("opted_in", false).remove("subscription").commit()
                status = status.copy(enabled = false, subscription_id = null)
            } else register(owner, force = !status.enabled)
        } else if (status.enabled && !prefs.getBoolean("opted_in", false)) {
            request("DELETE", installation())
            status = status.copy(enabled = false, subscription_id = null)
        }
        Log.i(ABSENCE_TAG, "status available=${status.available} enabled=${status.enabled}")
    }
    private suspend fun register(owner: String, force: Boolean = false) {
        val fcmToken = token()
        val key = "$owner|$fcmToken"
        if (!force && !AbsencePushPolicy.shouldRenew(renewedKey, key, renewedAt, System.currentTimeMillis())) return
        val result = request("PUT", installation(), fcmToken)
        // An in-flight registration must never restore opt-in after logout.
        if (account() != owner) return
        status = result
        prefs.edit().putString("owner", owner).putBoolean("opted_in", result.enabled).putString("subscription", result.subscription_id).commit()
        renewedKey = key
        renewedAt = System.currentTimeMillis()
    }
    fun setEnabled(enabled: Boolean) = perform {
        val owner = account() ?: return@perform
        if (enabled) {
            check(NotificationManagerCompat.from(context).areNotificationsEnabled())
            register(owner, force = true)
        } else {
            prefs.edit().putBoolean("opted_in", false).remove("subscription").commit()
            status = status.copy(enabled = false)
            prefs.edit().putString("pending_owner", owner).putString("pending_installation", installation()).commit()
            request("DELETE", installation())
            prefs.edit().remove("pending_owner").remove("pending_installation").commit()
            renewedKey = null
        }
    }
    fun onTokenChanged() { renewedKey = null; refresh() }
    suspend fun beforeSignOut() {
        // Wait for any in-flight enrollment, then revoke while MSAL can still authenticate.
        mutex.lock()
        try {
            val owner = account()
            val previous = installation()
            prefs.edit().putBoolean("opted_in", false).remove("subscription").putString("pending_owner", owner).putString("pending_installation", previous).commit()
            status = AbsenceStatus()
            pendingOpen.value = null
            renewedKey = null
            try {
                withTimeout(10_000) { request("DELETE", previous) }
                prefs.edit().remove("pending_owner").remove("pending_installation").commit()
            } catch (e: CancellationException) { if (e !is TimeoutCancellationException) throw e }
            catch (_: Exception) { Log.w(ABSENCE_TAG, "revocation deferred=true") }
            FirebaseMessaging.getInstance().deleteToken()
            prefs.edit().putString("installation", UUID.randomUUID().toString()).remove("owner").commit()
            val notifications = context.getSystemService(android.app.NotificationManager::class.java)
            notifications.activeNotifications.filter { it.tag?.startsWith("absence:") == true }.forEach {
                notifications.cancel(it.tag, it.id)
            }
        } finally { mutex.unlock() }
    }
    fun open(subscription: String?) { if (!subscription.isNullOrEmpty()) pendingOpen.value = subscription }
    fun consumeOpen(): Boolean {
        val subscription = pendingOpen.value ?: return false
        pendingOpen.value = null
        val valid = prefs.getString("owner", null) == account() && AbsencePushPolicy.accepts(prefs.getBoolean("opted_in", false), prefs.getString("subscription", null), subscription)
        if (valid) refreshGeneration.value += 1
        Log.i(ABSENCE_TAG, "open accepted=$valid")
        return valid
    }
    private fun perform(block: suspend () -> Unit) { scope.launch {
        if (!mutex.tryLock()) return@launch
        busy = true
        failed = false
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { failed = true; Log.w(ABSENCE_TAG, "sync failed type=${e.javaClass.simpleName}") }
        finally { busy = false; mutex.unlock() }
    } }
}
