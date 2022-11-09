package com.hippo.composeUi.composeSearch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.hippo.composeUi.composeExt.clickable2
import com.hippo.composeUi.searchLayout.AdvanceSearchItem
import com.hippo.composeUi.searchLayout.ComCategoryTableItem
import com.hippo.ehviewer.R
import com.hippo.viewModel.SearchScreenViewModel
import com.hippo.viewModel.SearchScreenViewModel.SearchAction.*

@Composable
fun CategoryTable(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
) {
    val categoryText = stringArrayResource(id = R.array.Category)
    val context = LocalContext.current
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
        state.categorySelected.forEachIndexed { index, item ->
            Row(modifier = Modifier
                .fillMaxWidth(columnsPercentage.value)
                .padding(horizontal = 5.dp)) {
                ComCategoryTableItem(categoryText[index], item) {
                    sendEvent(SearchScreenViewModel.SearchAction.ChangeCategorySelected(index))
                }

            }

        }
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MinRating(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable2(enabledIndication = false) {
            expanded = true
            if ( state.mMinRating == -1) {
                sendEvent(ChangeMinRating(2))
            }
        }
    ) {
        AdvanceSearchItem(stringResource(id = R.string.search_sr),  state.mMinRating != -1) {
            if (state.mMinRating != -1) {
                sendEvent(ChangeMinRating(-1))
            } else {
                sendEvent(ChangeMinRating(2))
            }
        }
        if ( state.mMinRating != -1) {
            Box(Modifier.wrapContentSize(Alignment.TopStart)) {
                AnimatedContent(targetState= state.mMinRating ) { targetCount ->
                    Text(
                        text = "$targetCount Star",
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(25))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    repeat(4) {
                        DropdownMenuItem(
                            text = { Text(" ${it + 2} 星") },
                            onClick = {
                                expanded = false
                                sendEvent(ChangeMinRating(it + 2))
                            },
                        )
                    }

                }

            }
        }
    }

}