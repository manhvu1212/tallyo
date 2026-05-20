package io.github.manhvu1212.tallyo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors

@Composable
fun TallyoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
    textAlign: TextAlign = TextAlign.Start,
    textColor: androidx.compose.ui.graphics.Color = TallyoColors.Text,
) {
    val shape = RoundedCornerShape(10.dp)
    val baseModifier = modifier
        .clip(shape)
        .background(TallyoColors.Surface)
        .border(1.dp, TallyoColors.Border, shape)
    val withFocus = if (focusRequester != null) baseModifier.focusRequester(focusRequester) else baseModifier

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = textColor, fontSize = 16.sp, textAlign = textAlign),
        cursorBrush = SolidColor(TallyoColors.Primary),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = withFocus.padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = TallyoColors.TextMuted, fontSize = 16.sp)
            }
            inner()
        },
    )
}

object Ime {
    val Done = KeyboardOptions(imeAction = ImeAction.Done)
    val Next = KeyboardOptions(imeAction = ImeAction.Next)
    val Number = KeyboardOptions(
        imeAction = ImeAction.Done,
        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
    )
}
