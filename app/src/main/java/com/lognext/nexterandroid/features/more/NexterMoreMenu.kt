package com.lognext.nexterandroid.features.more

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography

@Composable
internal fun NexterMoreMenu(displayName: String, onDismiss: () -> Unit, onInventory: () -> Unit, onSystems: () -> Unit, onWellbeing: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val name = displayName.ifBlank { stringResource(R.string.navigation_user) }
            val initials = name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").uppercase(Locale.getDefault())
            Box(Modifier.size(42.dp).background(NexterColors.Red.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Text(initials, color = NexterColors.Red, fontSize = NexterTypography.Avatar, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(name, color = NexterColors.primaryText(), fontSize = NexterTypography.Body, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
            val closeLabel = stringResource(R.string.navigation_close_menu)
            val closeColor = NexterColors.secondaryText()
            IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp).semantics { contentDescription = closeLabel }) {
                Canvas(Modifier.size(24.dp)) {
                    val stroke = 1.4.dp.toPx()
                    drawLine(closeColor, Offset(size.width * .33f, size.height * .33f), Offset(size.width * .67f, size.height * .67f), stroke, StrokeCap.Round)
                    drawLine(closeColor, Offset(size.width * .67f, size.height * .33f), Offset(size.width * .33f, size.height * .67f), stroke, StrokeCap.Round)
                }
            }
        }
        Divider(color = NexterColors.border())
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.navigation_employee).uppercase(Locale.getDefault()), color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Caption, letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 14.dp))
            Row(Modifier.fillMaxWidth()
                .clickable(onClick = onInventory).padding(14.dp).heightIn(min = 24.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(painterResource(R.drawable.ic_inventory_box), contentDescription = null, tint = NexterColors.Red, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.inventory_title), color = NexterColors.primaryText(), fontSize = NexterTypography.Body,
                    modifier = Modifier.weight(1f))
                Text("›", color = NexterColors.secondaryText())
            }
            Row(Modifier.fillMaxWidth().clickable(onClick = onWellbeing).padding(14.dp).heightIn(min = 24.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(painterResource(R.drawable.ic_wellbeing), contentDescription = null, tint = NexterColors.Red, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.wellbeing_title), color = NexterColors.primaryText(), fontSize = NexterTypography.Body, modifier = Modifier.weight(1f))
                Text("›", color = NexterColors.secondaryText())
            }
            Text(stringResource(R.string.support_it).uppercase(Locale.getDefault()), color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Caption, letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 14.dp).padding(top = 18.dp))
            Row(Modifier.fillMaxWidth().clickable(onClick = onSystems).padding(14.dp).heightIn(min = 24.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(painterResource(R.drawable.ic_inventory_headphones), contentDescription = null, tint = NexterColors.Red, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.support_create_ticket), color = NexterColors.primaryText(), fontSize = NexterTypography.Body, modifier = Modifier.weight(1f))
                Text("›", color = NexterColors.secondaryText())
            }
        }
    }
}
