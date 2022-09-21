package com.hippo.composeUi.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class DialogBaseSelectItemWithIconAdapter(
    @StringRes
    val text: Int,
    val icon: Int,
    val onClick: () -> Unit
)

@Composable
fun ComposeDialogWithSelectItem(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
    dialogSelectItemAdapter: ArrayList<DialogBaseSelectItemWithIconAdapter>
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {

        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = Modifier
                    .sizeIn(minWidth = MinWidth, maxWidth = MaxWidth)
                    .padding(DialogPadding)
            ) {
                Column {
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        title?.invoke()
                    }
                    Spacer(modifier = Modifier.padding(vertical = 8.dp))
                    dialogSelectItemAdapter.forEach {
                        Button(onClick = { it.onClick.invoke() }) {
                            Row(
                                Modifier
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    painter = painterResource(id = it.icon),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                                Text(text = stringResource(id = it.text), fontSize = 18.sp)
                            }
                        }
                    }


                }

            }
        }

    }
}

private val MinWidth = 280.dp
private val MaxWidth = 560.dp
private val DialogPadding = PaddingValues(all = 24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Button(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Surface(onClick = onClick, interactionSource = interactionSource) {
        content.invoke()
    }
}