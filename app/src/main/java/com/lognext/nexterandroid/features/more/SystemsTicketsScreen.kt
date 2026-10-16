package com.lognext.nexterandroid.features.more

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.features.common.EmployeeCategory
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography
import kotlinx.coroutines.CancellationException

@Composable
internal fun SystemsTicketsScreen(account: String, initialCategory: EmployeeCategory?, onCategoryResolved: (EmployeeCategory?) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var loading by remember { mutableStateOf(initialCategory == null) }
    var profileError by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var browserError by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    LaunchedEffect(initialCategory, retry) {
        if (category != null) return@LaunchedEffect
        loading = true
        profileError = false
        try {
            category = try {
                EmployeeCategory.fromJobTitle(AppDependencies.peopleService.me().jobTitle)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) {
                // Resolve only an exact email match; never infer a role from a similar name.
                val profile = AppDependencies.peopleService.searchStaff(account).people
                    .singleOrNull { it.email.orEmpty().trim().equals(account.trim(), ignoreCase = true) }
                EmployeeCategory.fromJobTitle(profile?.jobTitle)
            }
            onCategoryResolved(category)
            profileError = category == null
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { profileError = true }
        finally { loading = false }
    }
    Surface(color = NexterColors.pageBackground(), modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column {
            MenuScreenHeader(R.string.support_it, onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when {
                    category != null -> SystemsTicketForm.forCategory(category).forEach { form ->
                        SupportTicketCard(form) {
                            try {
                                CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(form.url))
                            } catch (_: ActivityNotFoundException) { browserError = true }
                        }
                    }
                    loading -> Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = NexterColors.Red, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.support_loading_profile), color = NexterColors.secondaryText())
                    }
                    profileError -> Card(backgroundColor = NexterColors.cardBackground(), shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.support_profile_error), color = NexterColors.secondaryText())
                            TextButton(onClick = { retry++ }) { Text(stringResource(R.string.inventory_retry), color = NexterColors.Red) }
                        }
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
internal fun MenuScreenHeader(title: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(NexterColors.cardBackground()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)) {
            Box(Modifier.size(38.dp).background(NexterColors.Red.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_menu_back), contentDescription = stringResource(R.string.inventory_back), tint = NexterColors.Red, modifier = Modifier.size(17.dp))
            }
        }
        Text(stringResource(title), color = NexterColors.primaryText(), fontSize = NexterTypography.ScreenTitle, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SupportTicketCard(form: SystemsTicketForm, onClick: () -> Unit) {
    val title = when (form) {
        SystemsTicketForm.StaffIncident, SystemsTicketForm.ConsultantIncident -> R.string.support_report_incident
        SystemsTicketForm.StaffRequest -> R.string.support_create_request
        SystemsTicketForm.StaffEquipment -> R.string.support_request_equipment
    }
    val subtitle = when (form) {
        SystemsTicketForm.StaffRequest -> R.string.support_request_description
        SystemsTicketForm.StaffEquipment -> R.string.support_equipment_description
        else -> null
    }
    val icon = when (form) {
        SystemsTicketForm.StaffIncident, SystemsTicketForm.ConsultantIncident -> R.drawable.ic_support_incident
        SystemsTicketForm.StaffRequest -> R.drawable.ic_support_request
        SystemsTicketForm.StaffEquipment -> R.drawable.ic_inventory_laptop
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), backgroundColor = NexterColors.cardBackground(), shape = RoundedCornerShape(18.dp), elevation = 0.dp) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).background(NexterColors.Red.copy(alpha = 0.1f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = NexterColors.Red, modifier = Modifier.size(25.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold, color = NexterColors.primaryText())
                subtitle?.let { Text(stringResource(it), style = MaterialTheme.typography.body2, color = NexterColors.secondaryText()) }
            }
            Icon(painterResource(R.drawable.ic_support_external), contentDescription = null, tint = NexterColors.secondaryText(), modifier = Modifier.size(16.dp))
        }
    }
}
