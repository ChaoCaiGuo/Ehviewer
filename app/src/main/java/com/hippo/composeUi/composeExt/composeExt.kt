package com.hippo.composeUi.composeExt

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import android.widget.LinearLayout
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.ComposeView
import androidx.paging.compose.LazyPagingItems
import com.hippo.composeUi.theme.EhViewerTheme
import com.hippo.ehviewer.ui.MainActivity

/**
 *  通过LinearLayout来作为compose的过渡方式
 *  不要直接使用ComposeView，因为直接添加ComposeView 会因为未测量子控件导致ComposeView的wrap_content为0dp
 */
fun LinearLayout.addComposeView(content: @Composable () -> Unit) {

    this.addView(ComposeView(context).apply {
        setContent {

            EhViewerTheme {

                content.invoke()

            }

        }
    })

}

fun <T : Any> LazyGridScope.items(
    items: LazyPagingItems<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(value: T?) -> Unit
) {
    items(
        count = items.itemCount,
        key = if (key == null) null else { index ->
            val item = items.peek(index)
            if (item == null) {
                PagingPlaceholderKey(index)
            } else {
                key.invoke(item)
            }
        },
        itemContent ={ index ->
            itemContent(items[index])
        }
    )
}

@SuppressLint("BanParcelableUsage")
private data class PagingPlaceholderKey(private val index: Int) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(index)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<PagingPlaceholderKey> =
            object : Parcelable.Creator<PagingPlaceholderKey> {
                override fun createFromParcel(parcel: Parcel) =
                    PagingPlaceholderKey(parcel.readInt())

                override fun newArray(size: Int) = arrayOfNulls<PagingPlaceholderKey?>(size)
            }
    }
}

val LocalMainActivity = staticCompositionLocalOf<MainActivity?> {
    error("CompositionLocal LocalMainContext not present")
}
