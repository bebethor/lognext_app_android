package com.lognext.nexterandroid.features.more

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.core.network.APIError
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.CancellationException
import java.util.Date
import java.text.DateFormat

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun InventoryScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf<InventoryAssignment?>(null) }
    var items by remember { mutableStateOf(emptyList<InventoryAssignment>()) }
    var loading by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val context = LocalContext.current
    LaunchedEffect(refresh) {
        loading = true
        error = null
        try {
            items = AppDependencies.inventoryService.myInventory().items
            loaded = true
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) {
            error = context.getString(when ((failure as? APIError.Http)?.statusCode) {
                404 -> R.string.inventory_unavailable
                401, 403 -> R.string.inventory_unauthorized
                else -> R.string.inventory_error
            })
        } finally { loading = false }
    }
    val refreshState = rememberPullRefreshState(refreshing = loading && loaded, onRefresh = {
        if (!loading && selected == null) {
            AppDependencies.clearResponseCache()
            refresh++
        }
    })
    BackHandler { if (selected != null) selected = null else onBack() }
    Surface(color = NexterColors.pageBackground(), modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column {
            MenuScreenHeader(R.string.inventory_title, onBack = { if (selected != null) selected = null else onBack() })
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp).pullRefresh(refreshState, enabled = selected == null && !loading)) {
            AnimatedContent(targetState = selected, transitionSpec = {
                val direction = if (targetState == null) -1 else 1
                slideInHorizontally(tween(300)) { it * direction } togetherWith
                    slideOutHorizontally(tween(300)) { -it * direction }
            }, label = "inventoryDetail") { detail ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(4.dp))
                    if (detail != null) {
                        InventoryDetails(detail)
                    } else {
                        if (loading && !loaded) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.inventory_loading))
                        }
                        error?.let {
                            Text(it, color = NexterColors.systemRed())
                            TextButton(onClick = { refresh++ }, enabled = !loading) { Text(stringResource(R.string.inventory_retry)) }
                        }
                        if (loaded && items.isEmpty() && error == null) {
                            Text(stringResource(R.string.inventory_empty), style = MaterialTheme.typography.h6)
                            Text(stringResource(R.string.inventory_empty_detail))
                        }
                        val today = Date()
                        InventoryPeriod.entries.forEach { period ->
                            val records = items.filter { it.period(today) == period }.sortedByDescending { it.start?.time }
                            if (records.isNotEmpty()) {
                                Text(stringResource(when (period) {
                                    InventoryPeriod.CURRENT -> R.string.inventory_current
                                    InventoryPeriod.UPCOMING -> R.string.inventory_upcoming
                                    InventoryPeriod.HISTORY -> R.string.inventory_history
                                }), style = MaterialTheme.typography.subtitle1)
                                records.forEach { item ->
                                    Card(modifier = Modifier.fillMaxWidth().clickable { selected = item }, backgroundColor = NexterColors.cardBackground(), shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
                                        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            InventoryIcon(item)
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Text(item.name, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = NexterColors.primaryText())
                                                item.model?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.body2, color = NexterColors.secondaryText()) }
                                                item.assetNumber?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.caption, color = NexterColors.secondaryText()) }
                                            }
                                            Icon(painterResource(R.drawable.ic_inventory_next), contentDescription = null, tint = NexterColors.secondaryText(), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (selected == null) PullRefreshIndicator(loading && loaded, refreshState, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun InventoryDetails(item: InventoryAssignment) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InventoryIcon(item, large = true)
        Text(item.name, style = MaterialTheme.typography.h5, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        item.model?.takeIf { it.isNotBlank() }?.let { Text(it, color = NexterColors.secondaryText(), textAlign = TextAlign.Center) }
        Text(stringResource(when (item.period()) {
            InventoryPeriod.CURRENT -> R.string.inventory_current
            InventoryPeriod.UPCOMING -> R.string.inventory_upcoming
            InventoryPeriod.HISTORY -> R.string.inventory_history
        }), style = MaterialTheme.typography.caption, color = NexterColors.Red,
            modifier = Modifier.background(NexterColors.Red.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp))
    }
    InventorySection(R.string.inventory_material) {
        InventoryField(R.string.inventory_name, item.name)
        if (!item.itemType.isNullOrBlank()) { Divider(); InventoryField(R.string.inventory_type, item.itemType) }
        if (!item.model.isNullOrBlank()) { Divider(); InventoryField(R.string.inventory_model, item.model) }
    }
    if (!item.serialNumber.isNullOrBlank() || !item.assetNumber.isNullOrBlank()) {
        InventorySection(R.string.inventory_identifiers) {
            if (!item.serialNumber.isNullOrBlank()) InventoryField(R.string.inventory_serial, item.serialNumber, copyable = true)
            if (!item.serialNumber.isNullOrBlank() && !item.assetNumber.isNullOrBlank()) Divider()
            if (!item.assetNumber.isNullOrBlank()) InventoryField(R.string.inventory_asset, item.assetNumber, copyable = true)
        }
    }
    InventorySection(R.string.inventory_assignment) {
        item.start?.let { InventoryField(R.string.inventory_from, displayDate(it)); Divider() }
        item.end?.let { InventoryField(R.string.inventory_to, displayDate(it)) }
            ?: Text(stringResource(R.string.inventory_no_end), style = MaterialTheme.typography.body2, color = NexterColors.secondaryText())
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun InventoryField(label: Int, value: String?, copyable: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText())
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.inventory_not_available))
            }
        }
        if (copyable && !value.isNullOrBlank()) TextButton(onClick = {
            clipboard.setText(AnnotatedString(value))
            Toast.makeText(context, context.getString(R.string.inventory_copied), Toast.LENGTH_SHORT).show()
        }) { Text(stringResource(R.string.inventory_copy)) }
    }
}

private fun displayDate(date: Date): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)

@Composable
private fun InventorySection(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = NexterColors.secondaryText())
        Card(backgroundColor = NexterColors.cardBackground(), shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

@Composable
private fun InventoryIcon(item: InventoryAssignment, large: Boolean = false) {
    val description = java.text.Normalizer.normalize("${item.itemType.orEmpty()} ${item.name} ${item.model.orEmpty()}", java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase(java.util.Locale.ROOT)
    val icon = when {
        listOf("portatil", "laptop", "notebook", "macbook").any { it in description } -> R.drawable.ic_inventory_laptop
        listOf("monitor", "pantalla", "display").any { it in description } -> R.drawable.ic_inventory_monitor
        listOf("auricular", "headphone", "headset").any { it in description } -> R.drawable.ic_inventory_headphones
        listOf("teclado", "keyboard").any { it in description } -> R.drawable.ic_inventory_keyboard
        listOf("raton", "mouse").any { it in description } -> R.drawable.ic_inventory_mouse
        listOf("telefono", "phone", "movil").any { it in description } -> R.drawable.ic_inventory_phone
        else -> R.drawable.ic_inventory_box
    }
    Box(Modifier.size(if (large) 78.dp else 52.dp).background(NexterColors.Red.copy(alpha = 0.1f), RoundedCornerShape(if (large) 22.dp else 15.dp)), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = null, tint = NexterColors.Red, modifier = Modifier.size(if (large) 36.dp else 25.dp))
    }
}
