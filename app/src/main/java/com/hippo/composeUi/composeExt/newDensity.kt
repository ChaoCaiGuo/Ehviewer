package com.hippo.composeUi.composeExt
/**
 *      compose屏幕适配方案 该项目尚未适配
 *      使用方法见最下面{@link SampleNewDensity}
 *
 */
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp


/**
 *  匹配单位
 */

sealed class DensityIn {
    object Dp : DensityIn()
    object Px : DensityIn()
}

/**
 *  匹配方向
 *  MatchDirection.Width   适应宽度
 *  MatchDirection.Height  适应高度
 */
sealed class MatchDirection {
    object Width : MatchDirection()
    object Height : MatchDirection()
}

/**
 * @param target             设计图大小
 * @param densityIn         设计图匹配方式有匹配Dp和Px
 * @param enableFontScale   文字适应系统缩放（可能会损坏布局）  默认开启
 * @param matchDirection    自匹配方式 适应宽度或是适应高度
 *
 */
@Composable
fun newDensity(
    target: Float = 375F,
    densityIn: DensityIn = DensityIn.Dp,
    enableFontScale: Boolean = true,
    matchDirection: MatchDirection = MatchDirection.Width,
    content: @Composable () -> Unit
) {

    var density by remember {
        mutableStateOf(0f)
    }
    var scaled by remember {
        mutableStateOf(1f)
    }
    scaled =
        if (enableFontScale) LocalDensity.current.fontScale else 1f

    val displayMetrics = LocalContext.current.resources.displayMetrics
    val match = remember {
        if (matchDirection is MatchDirection.Width) displayMetrics.widthPixels else displayMetrics.heightPixels
    }

    Log.i(
        "newDensity",
        "getNewDensity: 宽度px${displayMetrics.widthPixels},高度px${displayMetrics.heightPixels}",
    )
    Log.i(
        "newDensity",
        "getNewDensity: 宽度dp${displayMetrics.widthPixels / LocalDensity.current.density},高度dp${displayMetrics.heightPixels / LocalDensity.current.density}"
    )
    Log.i("newDensity", "getNewDensity: 原density ${LocalDensity.current.density}")
    Log.i("newDensity", "getNewDensity: 系统缩放比${scaled}")

    when (densityIn) {
        DensityIn.Dp -> {
            density = match / target
            Log.i("newDensity", "getNewDensity: 改进density  ${density}")
            CompositionLocalProvider(LocalDensity provides Density(density, scaled)) {
                Log.i(
                    "newDensity",
                    "newDensity:新的 1dp =${with(LocalDensity.current) { 1.dp.toPx() }}px"
                )
                content.invoke()
            }

        }
        DensityIn.Px -> {
            val density_reciprocal =
               ( match / LocalDensity.current.density)/ target
            Log.i("newDensity", "getNewDensity: 修正 1px=  $density_reciprocal dp")
            CompositionLocalProvider(LocalNewPx provides density_reciprocal) {
                content.invoke()
            }
        }
    }

}
val LocalNewPx = staticCompositionLocalOf<Float> {
    error("CompositionLocal LocalNewPx not present")
}

@Immutable
@JvmInline
value class Px(
    val value: Float
)


@Stable
inline val Int.Px: Dp
    @Composable
    get() = Dp(value = this.toFloat()* LocalNewPx.current)

@Stable
inline val Dp.sp: TextUnit
    @Composable
    get() = with(LocalDensity.current) {
        this@sp.toSp()
    }


class SampleNewDensity{
    @Preview
    @Composable
    fun dpSample(){
        //指定个设计图20Dp，用20Dp的红色正方形填充
        newDensity(target = 20f){
           Spacer(modifier = Modifier.size(20.dp).background(Color.Red))
        }
    }

    @Preview
    @Composable
    fun pxSample(){
        //指定个设计图20PX，用20Px的红色正方形填充
        newDensity(target = 20f, densityIn = DensityIn.Px){
            Spacer(modifier = Modifier.size(20.Px).background(Color.Red))
        }
    }

}
