package com.lognext.nexterandroid.features.more

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.core.network.APIError
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.CancellationException
import java.util.Date
import java.text.DateFormat

@Composable
internal fun InventoryDialog(onDismiss: () -> Unit) {
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
    Dialog(onDismissRequest = { if (selected != null) selected = null else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler(enabled = selected != null) { selected = null }
        Surface(color = NexterColors.cardBackground(), modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (selected != null) TextButton(onClick = { selected = null }) { Text(stringResource(R.string.inventory_back)) }
                    Text(selected?.name ?: stringResource(R.string.inventory_title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.h6)
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.parity_close)) }
                }
                key(selected?.id) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val detail = selected
                        if (detail != null) {
                            InventoryDetails(detail)
                        } else {
                            TextButton(enabled = !loading, onClick = { AppDependencies.clearResponseCache(); refresh++ }) {
                                Text(stringResource(R.string.inventory_refresh))
                            }
                            if (loading) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
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
                                        Card(modifier = Modifier.fillMaxWidth().clickable { selected = item }, backgroundColor = NexterColors.pageBackground()) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(item.name, style = MaterialTheme.typography.subtitle1)
                                                item.model?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.body2, color = NexterColors.secondaryText()) }
                                                item.start?.let { Text(stringResource(R.string.inventory_since, displayDate(it)), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText()) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryDetails(item: InventoryAssignment) {
    Text(stringResource(R.string.inventory_material), style = MaterialTheme.typography.subtitle1)
    InventoryField(R.string.inventory_name, item.name)
    InventoryField(R.string.inventory_type, item.itemType)
    InventoryField(R.string.inventory_model, item.model)
    Divider()
    Text(stringResource(R.string.inventory_identifiers), style = MaterialTheme.typography.subtitle1)
    InventoryField(R.string.inventory_serial, item.serialNumber, copyable = true)
    InventoryField(R.string.inventory_asset, item.assetNumber, copyable = true)
    Divider()
    Text(stringResource(R.string.inventory_assignment), style = MaterialTheme.typography.subtitle1)
    item.start?.let { InventoryField(R.string.inventory_from, displayDate(it)) }
    item.end?.let { InventoryField(R.string.inventory_to, displayDate(it)) }
        ?: Text(stringResource(R.string.inventory_no_end), style = MaterialTheme.typography.caption, color = NexterColors.secondaryText())
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
