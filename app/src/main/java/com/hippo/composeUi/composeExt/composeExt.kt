package com.hippo.composeUi.composeExt

import android.widget.LinearLayout
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.paging.compose.LazyPagingItems
import com.hippo.composeUi.theme.EhViewerTheme

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
    key: ((index: Int) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(value: T?) -> Unit
) {
    items(
        count = items.itemCount,
        key = key,
        itemContent ={ index ->
            itemContent(items[index])
        }
    )
}
