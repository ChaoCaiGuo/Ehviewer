package com.hippo.composeUi.searchLayout

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.hippo.composeUi.composeExt.addComposeView
import com.hippo.composeUi.settingsFragment.AdvanceSearchItem
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.io.UniFileInputStreamPipe
import com.hippo.unifile.UniFile
import com.hippo.util.BitmapUtils
import com.hippo.viewModel.SearchViewModel
import com.hippo.yorozuya.IOUtils
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream


class ImageSearchLayout @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(mContext, attrs, defStyleAttr) {
    private val TAG = this::class.java.simpleName

    private val viewModel =
        ViewModelProvider(mContext as MainActivity).get(SearchViewModel::class.java)
    private var selectImage: (() -> Unit)? = null
    private var mImagePath: String? = null

    init {
        addComposeView { composeImageSearchLayout(viewModel,selectImage) }
    }

    fun setSelectImage( onclick: (() -> Unit)){
        selectImage=onclick
    }
    fun setImageUri(imageUri: Uri?) {
        if (null == imageUri) {
            return
        }
        viewModel.image_path =imageUri

        val file = UniFile.fromUri(mContext, imageUri) ?: return
        try {
            val maxSize = context.resources.getDimensionPixelOffset(R.dimen.image_search_max_size)
            val bitmap =
                BitmapUtils.decodeStream(UniFileInputStreamPipe(file), maxSize, maxSize) ?: return
            val temp = AppConfig.createTempFile() ?: return

            // TODO ehentai image search is bad when I'm writing this line.
            // Re-compress image will make image search failed.
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


    @Throws(EhException::class)
    fun formatListUrlBuilder(builder: ListUrlBuilder) {
        if (null == mImagePath) {
            throw EhException(context.getString(R.string.select_image_first))
        }
        builder.imagePath = mImagePath
        builder.isUseSimilarityScan = viewModel.image_selected[0]
        builder.isOnlySearchCovers =  viewModel.image_selected[1]
        builder.isShowExpunged =      viewModel.image_selected[2]
    }

}


@Composable
fun composeImageSearchLayout(viewModel: SearchViewModel, onclick: (() -> Unit)?) {
    val imageSearchLayoutString = stringArrayResource(id = R.array.ImageSearchLayout)
    Column {
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

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(90.dp)) {
            itemsIndexed(viewModel.image_selected) { index, item ->
                AdvanceSearchItem(imageSearchLayoutString[index], item) {
                    viewModel.image_selected[index] = !viewModel.image_selected[index]
                }
            }
        }
    }


}