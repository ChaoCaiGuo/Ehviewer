package com.hippo.composeUi.searchLayout

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.hippo.util.ExceptionUtils
import com.hippo.viewModel.SearchViewModel
import kotlinx.coroutines.launch


@Composable
fun ComposeImageSearch(viewModel: SearchViewModel) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { result: Uri? ->
            viewModel.setImageUri(result)
        }
    val imageSearchOptionsStringList = stringArrayResource(id = R.array.ImageSearchOptions)
    val scope = rememberCoroutineScope()
    val errorSnackBar =  stringResource(id = R.string.error_cant_find_activity)

    Column {
        Text(
            text = stringResource(id = R.string.search_image),
            fontWeight = FontWeight.W900,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(17.dp))
        if (viewModel.mImageUri != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = viewModel.mImageUri,
                    contentDescription = null
                )
            }
        }

        Button(onClick = {
            try {
                launcher.launch(arrayOf("image/*"))
            } catch (e: Throwable) {
                ExceptionUtils.throwIfFatal(e)
                scope.launch {
                    viewModel.snackbarHostState.showSnackbar(errorSnackBar)
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.select_image),
                fontSize = 15.sp, color = Color.White
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(90.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(viewModel.imageSearchOptionsSelected) { index, item ->
                AdvanceSearchItem(imageSearchOptionsStringList[index], item) {
                    viewModel.imageSearchOptionsSelected[index] =
                        !viewModel.imageSearchOptionsSelected[index]
                }
            }
        }
    }

}