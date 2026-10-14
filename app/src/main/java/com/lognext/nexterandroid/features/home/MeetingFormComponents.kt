package com.lognext.nexterandroid.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography
import java.text.DateFormat
import java.util.Date

@Composable
internal fun MeetingCreationFrame(busy: Boolean, onDismiss: () -> Unit, scrollState: ScrollState, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = !busy)) {
        Surface(color = NexterColors.pageBackground(), modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Column {
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.cancel)) }
                Text(stringResource(R.string.parity_new_meeting), fontSize = NexterTypography.ScreenTitle, fontWeight = FontWeight.Bold,
                    color = NexterColors.primaryText(), modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp))
                Column(Modifier.weight(1f).verticalScroll(scrollState).padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            }
        }
    }
}

@Composable
internal fun MeetingFormSection(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        title?.let { Text(it.uppercase(), color = NexterColors.secondaryText(), fontSize = NexterTypography.Footnote, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) }
        Surface(shape = RoundedCornerShape(10.dp), color = NexterColors.cardBackground(), modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
internal fun MeetingFormDivider() = Divider(color = NexterColors.border(), modifier = Modifier.padding(start = 16.dp))

@Composable
internal fun MeetingFormText(value: String, onValueChange: (String) -> Unit, placeholder: String, enabled: Boolean,
    modifier: Modifier = Modifier, multiline: Boolean = false, attendeeSearch: Boolean = false) {
    BasicTextField(value = value, onValueChange = onValueChange, enabled = enabled,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        singleLine = !multiline, minLines = if (multiline) 3 else 1, maxLines = if (multiline) 8 else 1,
        textStyle = MaterialTheme.typography.body1.copy(color = NexterColors.primaryText(), fontSize = NexterTypography.Body),
        cursorBrush = SolidColor(NexterColors.Red),
        keyboardOptions = KeyboardOptions(capitalization = if (attendeeSearch) KeyboardCapitalization.None else KeyboardCapitalization.Sentences, autoCorrect = !attendeeSearch),
        decorationBox = { field -> Box { if (value.isEmpty()) Text(placeholder, color = NexterColors.tertiaryText(), fontSize = NexterTypography.Body); field() } })
}

@Composable
internal fun MeetingDateRow(label: String, date: Date, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = NexterColors.primaryText(), fontSize = NexterTypography.Body)
        Spacer(Modifier.weight(1f))
        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date),
            color = NexterColors.primaryText(), fontSize = NexterTypography.Callout, textAlign = TextAlign.End,
            modifier = Modifier.background(NexterColors.subtleBackground(), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp))
    }
}

@Composable
internal fun AgendaHeaderGlyph(plus: Boolean, label: String) {
    Canvas(Modifier.size(22.dp).semantics { contentDescription = label }) {
        val color = if (plus) Color.White else NexterColors.Red
        val thickness = 2.dp.toPx()
        if (plus) {
            drawLine(color, Offset(size.width * 0.25f, size.height * 0.5f), Offset(size.width * 0.75f, size.height * 0.5f), thickness, StrokeCap.Round)
            drawLine(color, Offset(size.width * 0.5f, size.height * 0.25f), Offset(size.width * 0.5f, size.height * 0.75f), thickness, StrokeCap.Round)
        } else {
            drawLine(color, Offset(size.width * 0.25f, size.height * 0.4f), Offset(size.width * 0.5f, size.height * 0.65f), thickness, StrokeCap.Round)
            drawLine(color, Offset(size.width * 0.5f, size.height * 0.65f), Offset(size.width * 0.75f, size.height * 0.4f), thickness, StrokeCap.Round)
        }
    }
}
