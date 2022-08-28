package com.hippo.composeUi.searchLayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.hippo.composeUi.composeExt.addComposeView
import com.hippo.composeUi.composeExt.pagerTabIndicatorOffset
import com.hippo.composeUi.theme.MainColor
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhConfig
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.io.UniFileInputStreamPipe
import com.hippo.unifile.UniFile
import com.hippo.util.BitmapUtils
import com.hippo.viewModel.SearchViewModel
import com.hippo.yorozuya.IOUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

val TAG = "SearchLayout"

class SearchLayout @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(mContext, attrs, defStyleAttr) {


    companion object {
        val CategoryEhConfigs = listOf(
            EhConfig.DOUJINSHI, EhConfig.MANGA,
            EhConfig.ARTIST_CG, EhConfig.GAME_CG,
            EhConfig.WESTERN, EhConfig.NON_H,
            EhConfig.IMAGE_SET, EhConfig.COSPLAY,
            EhConfig.ASIAN_PORN, EhConfig.MISC
        )
    }

    init {
        addComposeView { ComposeSearchLayout(viewModel!!, onSelectImage) }
    }


    private var viewModel:SearchViewModel? = null
    private var mImagePath: String? = null
    private var onSelectImage:(()->Unit)? =null

    fun setViewModel(viewModel:SearchViewModel){
        this.viewModel =viewModel
    }

    @SuppressLint("NonConstantResourceId")
    @Throws(EhException::class)
    fun formatListUrlBuilder(urlBuilder: ListUrlBuilder, query: String?) {
        urlBuilder.reset()
        when (viewModel?.mSearchMode) {
            0 -> {
                urlBuilder.mode = ListUrlBuilder.MODE_NORMAL
                urlBuilder.keyword = query
                urlBuilder.category = viewModel!!.getCategory()
                if (viewModel!!.enabledAdvance) {
                    urlBuilder.advanceSearch = viewModel!!.getAdvanceSearch()
                    urlBuilder.minRating = viewModel!!.minRating
                    urlBuilder.pageFrom = viewModel!!.searchPageNumber.PageFrom
                    urlBuilder.pageTo = viewModel!!.searchPageNumber.PageTo
                }
            }
            1 -> {
                urlBuilder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                formatListUrlBuilder(urlBuilder)
            }
        }
    }

    @Throws(EhException::class)
    fun formatListUrlBuilder(builder: ListUrlBuilder) {
        if (null == mImagePath) {
            throw EhException(context.getString(R.string.select_image_first))
        }
        builder.imagePath = mImagePath
        builder.isUseSimilarityScan = viewModel!!.imageSearchOptionsSelected[0]
        builder.isOnlySearchCovers = viewModel!!.imageSearchOptionsSelected[1]
        builder.isShowExpunged = viewModel!!.imageSearchOptionsSelected[2]
    }

    fun setImageUri(imageUri: Uri?) {
        if (null == imageUri) {
            return
        }
        viewModel!!.imageUri = imageUri

        val file = UniFile.fromUri(mContext, imageUri) ?: return
        try {
            val maxSize = context.resources.getDimensionPixelOffset(R.dimen.image_search_max_size)
            val bitmap =
                BitmapUtils.decodeStream(UniFileInputStreamPipe(file), maxSize, maxSize) ?: return
            val temp = AppConfig.createTempFile() ?: return

            var os: OutputStream? = null
            try {
                os = FileOutputStream(temp)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                mImagePath = temp.path
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } finally {
                IOUtils.closeQuietly(os)
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory ${e.message}")
        }
    }


    fun onSelectImage(event:(()->Unit)?){
        onSelectImage = event
    }

}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun ComposeSearchLayout(viewModel: SearchViewModel = viewModel(),onSelectImage:(()->Unit)? = null) {
    val pagerState = rememberPagerState()
    val tabRowString = listOf(
        stringResource(id = R.string.keyword_search),
        stringResource(id = R.string.image_search)
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.mSearchMode = page
            viewModel.verticalScroll.animateScrollTo(0)
        }
    }

    Column(modifier = Modifier.verticalScroll(viewModel.verticalScroll)) {
        HorizontalPager(
            count = tabRowString.size,
            state = pagerState,
            modifier = Modifier.animateContentSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> Column {
                    ComposeKeywordSearch(viewModel)
                    TabRow(pagerState, tabRowString)
                }
                1 -> Column {
                    CardPage { ComposeImageSearch(viewModel) { onSelectImage?.invoke() } }
                    TabRow(pagerState, tabRowString)
                }
                else -> {}
            }
        }

    }

}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TabRow(
    pagerState: PagerState,
    tabRowString: List<String>,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    TabRow(
        contentColor = MaterialTheme.colorScheme.MainColor,
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        },

        ) {
        tabRowString.forEachIndexed { index, title ->
            Tab(
                text = { Text(title) },
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }

                },
            )
        }
    }
}

@Composable
fun CardPage(content: @Composable () -> Unit) {
    Card(modifier = Modifier.padding(4.dp)) {
        Box(modifier = Modifier.padding(15.dp)) {
            content.invoke()
        }
    }
}

