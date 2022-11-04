package com.hippo.composeUi.composeSearch

import android.text.TextUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hippo.composeUi.composeExt.firstBaselineToTop
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
        if (!isImeShow) {
            focusedManager.clearFocus()
        }else{
            viewModel.sendEvent(UpdateSuggestions)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                ComposeSearchTextField(
                    state = state,
                    sendEvent = {searchAction -> viewModel.sendEvent(searchAction) }
                )
            }
            TextButton(onClick = {  }) {
                Text(text =  stringResource(id = R.string.search), fontSize = 17.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                ComposeSearchTextField(
                    state = state,
                    sendEvent = {searchAction -> viewModel.sendEvent(searchAction) }
                )
            }
            TextButton(onClick = {  }) {
                Text(text =  stringResource(id = R.string.search), fontSize = 17.sp)
            }
        }

        Box(modifier = Modifier.weight(1f)){
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
private fun ComposeSuggestion(
    state: SearchScreenViewModel.SearchState,
    sendEvent: (SearchScreenViewModel.SearchAction) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(5.dp),
        modifier = Modifier
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
                    .combinedClickable(onLongClick = {

                    }) {
                        sendEvent(OnClick(it))
                    }
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
fun ComposeSearchTextField(
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
            IconButton(onClick = { sendEvent(SetSearchText(""))}) {
                Icon(Icons.Default.Close, contentDescription = null)
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