package com.hippo.composeUi.composeExt

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
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role

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
        indication = if(enabledIndication) LocalIndication.current else null,
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
