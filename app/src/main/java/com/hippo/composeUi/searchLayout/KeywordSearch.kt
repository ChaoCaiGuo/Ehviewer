package com.hippo.composeUi.searchLayout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.hippo.composeUi.theme.TextColor
import com.hippo.ehviewer.R
import com.hippo.viewModel.SearchViewModel
import kotlinx.coroutines.launch


@Composable
fun ComposeKeywordSearch(viewModel: SearchViewModel) {

    Column {
        CardPage { SearchNormal(viewModel) }

        AnimatedVisibility(visible = viewModel.enabledAdvance) {
            CardPage { ComposeAdvanceSearchTable(viewModel) }
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(id = R.string.search_normal),
            fontWeight = FontWeight.W900,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(text = stringResource(id = R.string.select_all),
            modifier = Modifier.clickable {
                (0 until viewModel.categorySelected.size).forEach {
                    viewModel.categorySelected[it] = true
                }
            })
        Spacer(modifier = Modifier.width(20.dp))

        Text(text = stringResource(id = R.string.deselect_all),
            modifier = Modifier.clickable {
                (0 until viewModel.categorySelected.size).forEach {
                    viewModel.categorySelected[it] = false
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
    ) {
        CategoryTable(viewModel)
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
private fun CategoryTable(viewModel: SearchViewModel) {
    val categoryText = stringArrayResource(id = R.array.Category)
    val context =LocalContext.current
    val columnsPercentage = remember {
        derivedStateOf{
            if(context.resources.displayMetrics.let { it.heightPixels > it.widthPixels }){
                0.5f
            }
            else
                0.24f
        }
    }


    FlowRow(modifier = Modifier.fillMaxWidth()) {
        viewModel.categorySelected.forEachIndexed { index, item ->
            Row(modifier = Modifier
                .fillMaxWidth(columnsPercentage.value)
                .padding(horizontal = 5.dp)) {
                ComCategoryTableItem(categoryText[index], item) {
                    viewModel.categorySelected[index] = !viewModel.categorySelected[index]
                }

            }

        }
    }

}


/**
 * 下面是高级选项
 */
@Composable
fun ComposeAdvanceSearchTable(viewModel: SearchViewModel) {
    val advanceSearchOptionsStringList = stringArrayResource(id = R.array.AdvanceSearchOptions)

    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.search_advance),
            fontWeight = FontWeight.W900,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(modifier = Modifier.fillMaxWidth()) {
            viewModel.advanceOptionsSelected.forEachIndexed { index, item ->
                Box(modifier = Modifier.fillMaxWidth(0.5f)) {
                    AdvanceSearchItem(advanceSearchOptionsStringList[index], item) {
                        viewModel.advanceOptionsSelected[index] = !viewModel.advanceOptionsSelected[index]
                    }
                }

            }
        }

        MinRating(viewModel)
        PageNumber(viewModel)
    }

}


