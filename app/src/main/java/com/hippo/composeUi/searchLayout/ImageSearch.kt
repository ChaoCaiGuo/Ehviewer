package com.hippo.composeUi.searchLayout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hippo.ehviewer.R
import com.hippo.viewModel.SearchViewModel


@Composable
fun ComposeImageSearch(viewModel: SearchViewModel, onclick: (() -> Unit)?) {
    val imageSearchLayoutString = stringArrayResource(id = R.array.ImageSearchLayout)
    Column {
        Text(text = stringResource(id = R.string.search_image), fontWeight = FontWeight.W900, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(17.dp))
        if(viewModel.image_path != null){
            Box(contentAlignment= Alignment.Center, modifier = Modifier.fillMaxWidth()){
                AsyncImage(
                    model = viewModel.image_path,
                    contentDescription = null
                )
            }
        }

        Button(onClick = {onclick?.invoke()}, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.select_image),
                fontSize = 15.sp, color = Color.White
            )
        }

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(90.dp),userScrollEnabled =false) {
            itemsIndexed(viewModel.image_selected) { index, item ->
                AdvanceSearchItem(imageSearchLayoutString[index], item) {
                    viewModel.image_selected[index] = !viewModel.image_selected[index]
                }
            }
        }
    }

}