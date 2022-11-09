package com.hippo.composeUi.composeSearch

import android.text.TextUtils
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.flowlayout.FlowRow
import com.hippo.composeUi.composeExt.firstBaselineToTop
import com.hippo.composeUi.searchLayout.*
import com.hippo.composeUi.theme.SystemBarsColor
import com.hippo.composeUi.theme.TextColor
import com.hippo.ehviewer.R
import com.hippo.viewModel.SearchScreenViewModel
import com.hippo.viewModel.SearchScreenViewModel.SearchAction.*


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeSearch(viewModel: SearchScreenViewModel = hiltViewModel()){
    val state by viewModel.container.uiStateFlow.collectAsState()
    val focusedManager = LocalFocusManager.current
    val isImeShow = WindowInsets.isImeVisible
    LaunchedEffect(isImeShow) {
        viewModel.sendEvent(UpdateSuggestions)
        if (!isImeShow) {
            focusedManager.clearFocus()
        }
    }

    LaunchedEffect(true){
        viewModel.sendEvent(UpdateSuggestions)
        viewModel.sendEvent(ChangeCategorySelected(-1))
        viewModel.sendEvent(ChangeAdvanceOptionsSelected(-1))
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                ComposeSearchTextField(
                    state = state,
                    sendEvent = {searchAction -> viewModel.sendEvent(searchAction) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)){
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())) {
                ComposeSearchOptions(
                    state = state,
                    sendEvent = {searchAction -> viewModel.sendEvent(searchAction) }
                )
                ComposeSearchHistory(
                    state = state,
                    sendEvent = {searchAction -> viewModel.sendEvent(searchAction) }
                )
            }

            if (isImeShow && !TextUtils.isEmpty(state.mText.trim { it <= ' ' })) {
                ComposeSuggestion(
                    state = state,
                    sendEvent = {searchAction -> viewModel.sendEvent(searchAction) }
                )
            }
        }

    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComposeSearchHistory(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
) {
    Column(Modifier.padding( 20.dp)) {
        Text(
            text = stringResource(id = R.string.search_history),
            fontWeight = FontWeight.W600,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(modifier = Modifier.fillMaxWidth(), mainAxisSpacing = 20.dp,crossAxisSpacing=4.dp) {
            state.mSearchHistory.forEach { item ->
                Text(
                    modifier = Modifier
                        .widthIn(max = 100.dp)
                        .clip(RoundedCornerShape(15f))
                        .background(MaterialTheme.colorScheme.primary)
                        .combinedClickable(onLongClick = { sendEvent(OnLongClick(item)) })
                        { sendEvent(OnClick(item)) }
                        .padding(4.dp, 4.dp)
                    ,
                    text = item.mKeyword,
                    maxLines =1,
                    overflow = TextOverflow.Ellipsis,
                    color =Color.White
                )
            }
        }
    }

}
@Composable
private fun ComposeSearchOptions(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
){
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))
        CardPage { SearchNormalOptions(state,sendEvent) }

        AnimatedVisibility(
            visible = state.enabledAdvanceOptions,
        ) {
            CardPage {
                ComposeAdvanceSearchTable(state,sendEvent)
            }
        }
    }

    
}

@Composable
fun ComposeAdvanceSearchTable(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
) {
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

           state.mAdvanceOptions.forEachIndexed { index, item ->
                Box(modifier = Modifier.fillMaxWidth(0.5f)) {
                    AdvanceSearchItem(advanceSearchOptionsStringList[index], item) {
                        sendEvent(ChangeAdvanceOptionsSelected(index))
                    }
                }

            }
        }

        MinRating(state,sendEvent)

    }

}
@Composable
private fun ComposeSuggestion(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(5.dp),
        modifier = Modifier
            .padding(
                bottom = WindowInsets.ime
                    .asPaddingValues()
                    .calculateBottomPadding() + 40.dp
            )
            .clip(RoundedCornerShape(bottomEnd = 20.dp, bottomStart = 20.dp))
            .background(MaterialTheme.colorScheme.SystemBarsColor),
    ) {

        items(
            state.mSuggestionList,
            key = { item ->
                item.mKeyword
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 15.dp)
                    .height(45.dp)
                    .clickable { sendEvent(OnClick(it)) }
            ) {
                if (it.mHint != "") {
                    Text(
                        text = it.mKeyword,
                        fontSize = 20.sp,
                        modifier = Modifier.firstBaselineToTop(20.dp),
                        letterSpacing = 0.sp,
                        color = MaterialTheme.colorScheme.TextColor
                    )
                    Text(
                        text = it.mHint,
                        fontSize = 16.sp,
                        modifier = Modifier.firstBaselineToTop(14.dp),
                        color = Color.Gray
                    )
                } else {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(
                            text = it.mKeyword,
                            fontSize = 20.sp,
                            modifier = Modifier.firstBaselineToTop(20.dp),
                            letterSpacing = 0.sp,
                            color = MaterialTheme.colorScheme.TextColor
                        )
                    }

                }
                Divider(Modifier.fillMaxWidth())
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeSearchTextField(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
) {

    OutlinedTextField(
        value = state.mText,
        onValueChange = {
            sendEvent(SetSearchText(it))
        },
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .height(56.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(25),
        singleLine = true,
        placeholder = {
            Text(
                stringResource(id = R.string.search),
                fontSize = 20.sp,
                fontWeight = FontWeight.W400,
                color = Color.Gray
            )
        },
        trailingIcon = {
            if(!TextUtils.isEmpty(state.mText)){
                IconButton(onClick = { sendEvent(SetSearchText(""))}) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }

        },
        textStyle = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.W300,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { }),
        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = MaterialTheme.colorScheme.surface),
    )



}

@Composable
private fun SearchNormalOptions(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
) {
    val enableAdvanceString =
        if (state.enabledAdvanceOptions) stringResource(id = R.string.search_disable_advance)
        else stringResource(id = R.string.search_enable_advance)
    var enableAdvanceString1 by remember { mutableStateOf("-> $enableAdvanceString <-") }
    LaunchedEffect(state.enabledAdvanceOptions) {
        enableAdvanceString1 = if (state.enabledAdvanceOptions) {
            "-> $enableAdvanceString <-"
        } else {
            "<- $enableAdvanceString ->"
        }
    }

    Column {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.search_normal),
                fontWeight = FontWeight.W900,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.select_all),
                modifier = Modifier.clickable { sendEvent(ChangeAllCategorySelected(true)) }
            )
            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = stringResource(id = R.string.deselect_all),
                modifier = Modifier.clickable { sendEvent(ChangeAllCategorySelected(false)) }
            )
        }
        Spacer(modifier = Modifier.height(17.dp))

        CategoryTable(state,sendEvent)

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = enableAdvanceString1,
            modifier = Modifier
                .clickable { sendEvent(ChangeEnabledAdvanceOptions) }
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.TextColor,
            textAlign = TextAlign.Center)
    }
}

