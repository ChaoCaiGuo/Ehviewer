package com.hippo.composeUi.composeExt

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/**
 * 去除水波纹的clickable
 */
fun Modifier.clickable2(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    enabledIndication: Boolean = true,
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickable2"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    }
) {
    Modifier.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = onClick,
        role = role,
        indication = if (enabledIndication) LocalIndication.current else null,
        interactionSource = remember { MutableInteractionSource() }
    )
}


/**
 *  监听手指 按下 和 抬起 事件
 */
fun Modifier.touchEvent(onTouchDown: () -> Unit = {}, onTouchUp: () -> Unit = {}): Modifier =
    this.pointerInput(Unit) {
        forEachGesture {
            awaitPointerEventScope {

                val downPointer = awaitFirstDown()
                if (downPointer.changedToDown()) {
                    Log.i("ACTION", "ACTION: 按下")
                    onTouchDown.invoke()
                }
                while (true) {
                    val event = awaitDragOrCancellation(downPointer.id) ?: break
                    if (event.changedToUp()) {
                        Log.i("ACTION", "ACTION: 抬起")
                        // 所有手指均已抬起
                        onTouchUp.invoke()
                        break
                    }
                }

            }
        }

    }

/**
 * 页面百分比位移，可以用来作出入场动画
 */
@SuppressLint("ModifierFactoryUnreferencedReceiver")
fun Modifier.percentOffSet(X: Float, Y: Float): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val offsetx = (X * placeable.width).roundToInt()
            val offsety = (Y * placeable.height).roundToInt()
            placeable.placeRelative(offsetx, offsety)

        }

    }

fun Modifier.firstBaselineToTop(firstBaselineToTop: Dp): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        check(placeable[FirstBaseline] != AlignmentLine.Unspecified)
        val firstBaseline = placeable[FirstBaseline]
        val placeableY = firstBaselineToTop.roundToPx() - firstBaseline
        val height = placeable.height + placeableY
        layout(placeable.width, height) {
            placeable.placeRelative(0, placeableY)
        }
    }

