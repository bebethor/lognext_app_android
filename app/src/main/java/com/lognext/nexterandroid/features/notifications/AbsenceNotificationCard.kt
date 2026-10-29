package com.lognext.nexterandroid.features.notifications

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.ui.theme.NexterColors

@Composable
fun AbsenceNotificationCard() {
    val controller = AppDependencies.absenceNotifications
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.setEnabled(true)
    }
    LaunchedEffect(Unit) { controller.refresh() }
    if (!controller.status.available && !controller.failed) return
    Card(backgroundColor = NexterColors.cardBackground(), modifier = Modifier.fillMaxWidth(), elevation = 0.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.absence_push_title), modifier = Modifier.weight(1f), color = NexterColors.primaryText())
                if (controller.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Switch(checked = controller.status.enabled, enabled = !controller.busy && controller.status.available,
                    onCheckedChange = { enabled ->
                        if (!enabled) controller.setEnabled(false)
                        else if (NotificationManagerCompat.from(context).areNotificationsEnabled()) controller.setEnabled(true)
                        else if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else openNotificationSettings(context)
                    }, colors = SwitchDefaults.colors(checkedThumbColor = NexterColors.Red))
            }
            Text(stringResource(R.string.absence_push_explanation), color = NexterColors.primaryText())
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                TextButton(onClick = { openNotificationSettings(context) }) {
                    Text(stringResource(R.string.absence_push_settings), color = NexterColors.Red)
                }
            }
            if (controller.failed) {
                Text(stringResource(R.string.absence_push_error), color = NexterColors.primaryText())
                TextButton(onClick = { controller.refresh() }, enabled = !controller.busy) { Text(stringResource(R.string.absence_push_retry), color = NexterColors.Red) }
            }
        }
    }
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
    context.startActivity(intent)
}
