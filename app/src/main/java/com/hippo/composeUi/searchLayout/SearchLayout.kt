package com.hippo.composeUi.searchLayout

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
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
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhConfig
import com.hippo.viewModel.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        addComposeView { ComposeSearchLayout(viewModel!!) }
    }

    var viewModel: SearchViewModel? = null

}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun ComposeSearchLayout(viewModel: SearchViewModel = viewModel()) {
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
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> Column {
                    ComposeKeywordSearch(viewModel)
                    TabRow(pagerState, tabRowString)
                }
                1 -> Column {
                    CardPage { ComposeImageSearch(viewModel) { viewModel.onSelectImage?.invoke() } }
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

