package com.hippo.composeUi.composeExt

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.InputStream

/**
 * 谷歌官方的解决方案也是这样bitmap转ImageBitmap (ˉ▽ˉ；)...
 */
object ImageBitmapFactory {
    fun docodeFiles(pathName:String): ImageBitmap {
        return  BitmapFactory.decodeFile(pathName).asImageBitmap()
    }

    fun decodeStream(inputStream: InputStream): ImageBitmap {
        return  BitmapFactory.decodeStream(inputStream).asImageBitmap()
    }


}