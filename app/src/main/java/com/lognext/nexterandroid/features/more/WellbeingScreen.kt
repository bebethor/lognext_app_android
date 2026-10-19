package com.lognext.nexterandroid.features.more

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.lognext.nexterandroid.ui.theme.NexterTypography
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.json.JSONArray

@Composable
internal fun WellbeingMenuItems() {
    val context = LocalContext.current
    val services by produceState<List<WellbeingService>?>(null, context) {
        value = withContext(Dispatchers.IO) {
            try {
                val entries = JSONArray(context.assets.open("wellbeing-services.json").bufferedReader().use { it.readText() })
                val seen = mutableSetOf<String>()
                (0 until entries.length()).mapNotNull { index ->
                    val entry = entries.optJSONObject(index) ?: return@mapNotNull null
                    WellbeingService.create(entry.optString("id"), entry.optString("name"), entry.optString("summary"), entry.optString("bookingURL"))
                        ?.takeIf { seen.add(it.id) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { emptyList() }
        }
    }
    var browserError by remember { mutableStateOf(false) }
    if (!services.isNullOrEmpty()) {
        Text(stringResource(R.string.wellbeing_title).uppercase(java.util.Locale.getDefault()),
            color = NexterColors.secondaryText(), fontSize = NexterTypography.Caption,
            letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 14.dp).padding(top = 18.dp))
        services.orEmpty().forEach { service ->
            val serviceName = when (service.id) {
                "manicure-pedicure" -> stringResource(R.string.wellbeing_manicure)
                "physiotherapy" -> stringResource(R.string.wellbeing_physio)
                else -> service.name
            }
            Row(Modifier.fillMaxWidth().clickable {
                try {
                    CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(service.bookingURL))
                } catch (_: ActivityNotFoundException) { browserError = true }
            }.padding(14.dp).heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(painterResource(R.drawable.ic_wellbeing), contentDescription = null,
                    tint = NexterColors.Red, modifier = Modifier.size(22.dp))
                Text(serviceName, color = NexterColors.primaryText(), fontSize = NexterTypography.Body,
                    modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_support_external), contentDescription = null,
                    tint = NexterColors.secondaryText(), modifier = Modifier.size(16.dp))
            }
        }
    }
    if (browserError) AlertDialog(onDismissRequest = { browserError = false },
        text = { Text(stringResource(R.string.support_browser_error)) },
        confirmButton = { TextButton(onClick = { browserError = false }) { Text(stringResource(android.R.string.ok)) } })
}
