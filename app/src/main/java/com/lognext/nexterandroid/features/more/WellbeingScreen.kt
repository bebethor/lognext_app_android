package com.lognext.nexterandroid.features.more

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.json.JSONArray

@Composable
internal fun WellbeingScreen(onBack: () -> Unit) {
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
    BackHandler(onBack = onBack)
    Surface(color = NexterColors.pageBackground(), modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column {
            MenuScreenHeader(R.string.wellbeing_title, onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                WellbeingCard {
                    Icon(painterResource(R.drawable.ic_wellbeing), contentDescription = null, tint = NexterColors.Red, modifier = Modifier.size(44.dp))
                    Text(stringResource(R.string.wellbeing_hero), style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = NexterColors.primaryText())
                    Text(stringResource(R.string.wellbeing_description), color = NexterColors.secondaryText())
                }
                when {
                    services == null -> CircularProgressIndicator(color = NexterColors.Red)
                    services.orEmpty().isEmpty() -> WellbeingCard {
                        Text(stringResource(R.string.wellbeing_pending), style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = NexterColors.primaryText())
                        Text(stringResource(R.string.wellbeing_empty), color = NexterColors.secondaryText())
                    }
                    else -> {
                        Text(stringResource(R.string.wellbeing_services), style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = NexterColors.primaryText())
                        services.orEmpty().forEach { service ->
                            WellbeingCard(Modifier.clickable {
                                try {
                                    CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(service.bookingURL))
                                } catch (_: ActivityNotFoundException) { browserError = true }
                            }) {
                                Text(service.name, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = NexterColors.primaryText())
                                if (service.summary.isNotBlank()) Text(service.summary, color = NexterColors.secondaryText())
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.wellbeing_book), color = NexterColors.Red, modifier = Modifier.weight(1f))
                                    Icon(painterResource(R.drawable.ic_support_external), contentDescription = null, tint = NexterColors.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Text(stringResource(R.string.wellbeing_provider), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText())
                    }
                }
            }
        }
    }
    if (browserError) AlertDialog(onDismissRequest = { browserError = false },
        text = { Text(stringResource(R.string.support_browser_error)) },
        confirmButton = { TextButton(onClick = { browserError = false }) { Text(stringResource(android.R.string.ok)) } })
}

@Composable
private fun WellbeingCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), backgroundColor = NexterColors.cardBackground(), shape = RoundedCornerShape(20.dp), elevation = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
