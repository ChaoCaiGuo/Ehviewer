package com.hippo.composeUi.composeExt

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * 页面百分比位移，可以用来作出入场动画
 */
@SuppressLint("ModifierFactoryUnreferencedReceiver")
fun Modifier.percentOffSet(X:Float, Y:Float): Modifier =
    this.layout { measurable, constraints ->
        val placeable =measurable.measure(constraints)
        layout(placeable.width,placeable.height){
            val offsetx=(X*placeable.width).roundToInt()
            val offsety=(Y*placeable.height).roundToInt()
            placeable.placeRelative(offsetx,offsety)

        }

    }