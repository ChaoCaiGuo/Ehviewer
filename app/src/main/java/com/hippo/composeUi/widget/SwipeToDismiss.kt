package com.hippo.composeUi.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

@Composable
fun SwipeToDismiss(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    dismissThresholds: Float = 0.5f,
    content: @Composable () -> Unit
) {
    var width by remember {
        mutableStateOf(0)
    }

    var offset by remember {
        mutableStateOf(0)
    }

    var offsetOut by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(offsetOut) {
        if (offsetOut) {
            delay(300)
            onDismiss.invoke()
        }
    }

    val animateOffsetWidth by animateIntAsState(targetValue = offset) {
        if (it == -width) {
            offsetOut = true
        }
    }

    val draggableState = rememberDraggableState {
        offset = (offset + it)
            .coerceIn(-width.toFloat(), 0f)
            .toInt()
    }

    AnimatedVisibility(visible = !offsetOut) {
        Surface(modifier = modifier
            .let {
                if (width == 0)
                    it.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        width = placeable.width
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                else it
            }
            .offset {
                IntOffset(animateOffsetWidth, 0)
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = draggableState,
                onDragStopped = {
                    offset = if (offset < -width.toFloat() * dismissThresholds) {
                        -width
                    } else {
                        0
                    }
                }
            )
        ) {

            content.invoke()

        }
    }


}