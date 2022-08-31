package com.hippo.composeUi.widget

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.hippo.ehviewer.R

/**
 * 这个OutlinedTextField的isError为true时，会聚焦到控件上
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextFieldFix(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester ,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.outlinedShape,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {

    LaunchedEffect(isError){
        if(isError)
            focusRequester.requestFocus()
    }
    Column {
        OutlinedTextField(
            value,
            onValueChange,
            modifier.focusRequester(focusRequester),
            enabled,
            readOnly,
            textStyle,
            label,
            placeholder,
            leadingIcon,
            trailingIcon,
            isError,
            visualTransformation,
            keyboardOptions,
            keyboardActions,
            singleLine,
            maxLines,
            interactionSource,
            shape,
            colors
        )
        if(isError)
            Text(text = stringResource(id = R.string.text_is_empty), color =  Color(0xFFB62E2A))
    }


}