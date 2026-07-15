package com.lognext.nexterandroid.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography

@Composable
fun FeaturePlaceholder(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexterColors.pageBackground())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, color = NexterColors.primaryText(), fontSize = NexterTypography.ScreenTitle)
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = NexterColors.secondaryText(),
                fontSize = NexterTypography.Body,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
