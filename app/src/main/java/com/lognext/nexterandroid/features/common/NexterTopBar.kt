package com.lognext.nexterandroid.features.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography
import com.lognext.nexterandroid.ui.theme.isNexterDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NexterTopBar(
    displayName: String,
    onSignOut: () -> Unit
) {
    val date = remember {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date()).replace(".", "")
    }
    val isDark = isNexterDarkTheme()
    val logoRes = if (isDark) R.drawable.lognext_logo_negative else R.drawable.lognext_logo
    val topBarBackground = if (isDark) NexterColors.cardBackground() else NexterColors.pageBackground()
    val statusBarHeight = systemBarHeight("status_bar_height")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarBackground)
            .padding(top = statusBarHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "Lognext",
                modifier = Modifier
                    .height(30.dp)
                    .width(132.dp)
                    .weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = date,
                color = if (isDark) Color.White.copy(alpha = 0.72f) else NexterColors.Navy.copy(alpha = 0.35f),
                fontSize = NexterTypography.TopBarDate
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexterColors.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials(displayName),
                    color = Color.White,
                    fontSize = NexterTypography.Avatar,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, NexterColors.Red.copy(alpha = 0.7f)),
                modifier = Modifier.size(35.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logout),
                    contentDescription = "Cerrar sesión",
                    tint = NexterColors.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Divider(color = NexterColors.border(), thickness = 1.dp)
    }
}

@Composable
private fun systemBarHeight(resourceName: String): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    val resourceId = context.resources.getIdentifier(resourceName, "dimen", "android")
    val heightPx = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    return with(density) { heightPx.toDp() }
}

private fun initials(displayName: String): String {
    return displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "JA" }
}
