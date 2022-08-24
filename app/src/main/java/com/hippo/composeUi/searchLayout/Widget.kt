package com.hippo.composeUi.searchLayout

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.composeUi.composeExt.clickable2
import com.hippo.composeUi.theme.CheckBox
import com.hippo.composeUi.theme.TextColor
import com.hippo.ehviewer.R
import com.hippo.viewModel.SearchViewModel
import com.hippo.yorozuya.NumberUtils

/**
 *  checkBox带文本
 */
@Composable
fun AdvanceSearchItem(text: String, checked: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable2(enabledIndication = false){onClick.invoke()}) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onClick.invoke() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.CheckBox)
        )
        Text(text = text, color = MaterialTheme.colorScheme.TextColor)

    }
}

/**
 *  搜索页面的页数
 */
@Composable
fun PageNumber(viewModel: SearchViewModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        var enablePageMunber by rememberSaveable { mutableStateOf(!((viewModel.advance_pageMunber.PageFrom == -1) and (viewModel.advance_pageMunber.PageTo == -1))) }
        var pageFrom by remember { mutableStateOf(  if(viewModel.advance_pageMunber.PageFrom !=-1) viewModel.advance_pageMunber.PageFrom.toString() else "") }
        var pageTo   by remember { mutableStateOf(  if(viewModel.advance_pageMunber.PageTo   !=-1) viewModel.advance_pageMunber.PageTo.toString() else "") }

        AdvanceSearchItem(stringResource(id = R.string.search_sp), enablePageMunber) {
            enablePageMunber = !enablePageMunber
            if (!enablePageMunber) {
                pageFrom = ""
                pageTo = ""
                viewModel.advance_pageMunber.PageFrom = -1
                viewModel.advance_pageMunber.PageTo = -1
            }

        }
        if (enablePageMunber) {
            pageTextField(pageFrom) { text ->
                pageFrom = text
                viewModel.advance_pageMunber.PageFrom =
                    NumberUtils.parseIntSafely(text, -1)
            }
            Text(text = stringResource(id = R.string.search_sp_to))
            pageTextField(pageTo) { text ->
                pageTo = text
                viewModel.advance_pageMunber.PageTo =
                    NumberUtils.parseIntSafely(text, -1)
            }
        }

    }
}

/**
 *  自定义TextField用来输入age
 */
@Composable
private fun pageTextField(text: String, setText: (text: String) -> Unit) {
    val controller = TextStyle(
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
    BasicTextField(
        value = text,
        onValueChange = setText,
        textStyle = controller,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .size(40.dp),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.border(3.dp, MaterialTheme.colorScheme.CheckBox, RoundedCornerShape(25))
            ) {
                innerTextField()
            }
        },

        )
}

/**
 *  最低评分
 */
@Composable
fun MinRating(viewModel: SearchViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var enableMinRating by rememberSaveable { mutableStateOf(viewModel.advance_minRating != -1) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable2(enabledIndication = false) {
            expanded = true
            if (!enableMinRating) {
                viewModel.advance_minRating = 2
            }
            enableMinRating = true
        }
    ) {
        AdvanceSearchItem(stringResource(id = R.string.search_sr), enableMinRating) {
            enableMinRating = !enableMinRating
            if (!enableMinRating) {
                viewModel.advance_minRating = -1
            } else {
                viewModel.advance_minRating = 2
            }
        }
        if (enableMinRating) {
            Box(Modifier.wrapContentSize(Alignment.TopStart)) {
                Text(
                    text = "${viewModel.advance_minRating} Star",
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(25))
                        .background(MaterialTheme.colorScheme.CheckBox)
                        .padding(4.dp)

                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    repeat(4) {
                        DropdownMenuItem(
                            text = { Text(" ${it + 2} 星") },
                            onClick = {
                                expanded = false
                                viewModel.advance_minRating = it + 2
                            },
                        )
                    }

                }

            }
        }
    }

}

/**
 *  自定义Category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComCategoryTableItem(text: String, selected: Boolean, onClick: () -> Unit) {
    val temText = buildAnnotatedString {
        if (selected)
            append(text)
        else
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append(text)
            }
    }
    ElevatedFilterChip(
        selected = selected,
        onClick = { onClick.invoke() },
        label = { Text(temText, color = if (selected) Color.White else Color.Unspecified) },
        leadingIcon = {
            if (!selected) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                )
            }

        },
        colors = FilterChipDefaults.elevatedFilterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary
        )
    )
}
