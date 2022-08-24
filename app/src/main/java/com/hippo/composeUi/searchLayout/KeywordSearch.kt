package com.hippo.composeUi.searchLayout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.composeUi.theme.TextColor
import com.hippo.ehviewer.R
import com.hippo.viewModel.SearchViewModel
import kotlinx.coroutines.launch


@Composable
fun ComposeKeywordSearch(viewModel: SearchViewModel) {

    Column {
        CardPage { SearchNormal(viewModel) }

        AnimatedVisibility(visible = viewModel.enabledAdvance) {
            CardPage { ComAdvanceSearchTable(viewModel) }
        }

    }

}

@Composable
private fun SearchNormal(viewModel: SearchViewModel) {
    Column {
        SearchNormalTitle(viewModel)
        Spacer(modifier = Modifier.height(17.dp))
        SearchNormalCategory(viewModel)
    }
}

@Composable
private fun SearchNormalTitle(viewModel: SearchViewModel) {
    Row {
        Text(
            text = stringResource(id = R.string.search_normal),
            fontWeight = FontWeight.W900,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(text = stringResource(id = R.string.select_all),
            modifier = Modifier.clickable {
                (0 until viewModel.category_selected.size).forEach {
                    viewModel.category_selected[it] = true
                }
            })
        Spacer(modifier = Modifier.width(20.dp))

        Text(text = stringResource(id = R.string.deselect_all),
            modifier = Modifier.clickable {
                (0 until viewModel.category_selected.size).forEach {
                    viewModel.category_selected[it] = false
                }
            })
    }
}


@Composable
private fun SearchNormalCategory(viewModel: SearchViewModel) {
    val enableAdvanceString =
        if (viewModel.enabledAdvance) stringResource(id = R.string.search_disable_advance)
        else stringResource(id = R.string.search_enable_advance)
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxWidth()
            .height(270.dp)
    ) {
        ComCategoryTable(viewModel)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        )
        Text(text = "→$enableAdvanceString←",
            modifier = Modifier
                .clickable {
                    viewModel.enabledAdvance = !viewModel.enabledAdvance
                    scope.launch {
                        viewModel.verticalScroll.animateScrollTo(0)
                    }

                }
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.TextColor,
            textAlign = TextAlign.Center)
    }


}

@Composable
private fun ComCategoryTable(viewModel: SearchViewModel) {
    val categoryText = stringArrayResource(id = R.array.Category)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.wrapContentSize(Alignment.TopCenter),
        userScrollEnabled = false
    ) {
        itemsIndexed(viewModel.category_selected) { index: Int, item: Boolean ->
            ComCategoryTableItem(categoryText[index], item) {
                viewModel.category_selected[index] = !viewModel.category_selected[index]
            }
        }
    }
}


/**
 * 下面是高级选项
 */
@Composable
fun ComAdvanceSearchTable(viewModel: SearchViewModel) {
    val advanceSearchString = stringArrayResource(id = R.array.AdvanceSearch)

    Column(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Text(
            text = stringResource(id = R.string.search_advance),
            fontWeight = FontWeight.W900,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2), userScrollEnabled = false) {
            itemsIndexed(viewModel.advance_selected) { index, item ->
                AdvanceSearchItem(advanceSearchString[index], item) {
                    viewModel.advance_selected[index] = !viewModel.advance_selected[index]
                }
            }
        }
        MinRating(viewModel)
        PageNumber(viewModel)
    }

}


