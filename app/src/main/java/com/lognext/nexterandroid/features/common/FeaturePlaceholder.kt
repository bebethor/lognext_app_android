package com.lognext.nexterandroid.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.ui.theme.NexterColors

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
        Text(text = title, color = NexterColors.primaryText(), style = MaterialTheme.typography.h4)
        Text(
            text = subtitle,
            color = NexterColors.secondaryText(),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
