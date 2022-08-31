package com.hippo.viewModel

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.hippo.composeUi.searchLayout.SearchLayout
import com.hippo.composeUi.searchLayout.TAG
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.io.UniFileInputStreamPipe
import com.hippo.unifile.UniFile
import com.hippo.util.BitmapUtils
import com.hippo.yorozuya.IOUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {
    val categorySelected = mutableStateListOf<Boolean>()
    val advanceOptionsSelected = mutableStateListOf<Boolean>()
    val imageSearchOptionsSelected = mutableStateListOf<Boolean>()

    var minRating by mutableStateOf( -1)
    var searchPageNumber = pageMunber(-1, -1)
    var mImageUri by mutableStateOf<Uri?>(null)
    var enabledAdvance by mutableStateOf(false)
    val verticalScroll = ScrollState(initial = 0)
    var mSearchMode by mutableStateOf(0)
    private var mImagePath: String? = null
    var onSelectImage: (() -> Unit)? = null


    init {

        imageSearchOptionsSelected.add(true)
        imageSearchOptionsSelected.add(false)
        imageSearchOptionsSelected.add(false)

        repeat(10) {
            categorySelected.add(true)
        }

        (0 until EhApplication.getInstance().resources.getStringArray(R.array.AdvanceSearchOptions).size).forEach {
            if (it < 2)
                advanceOptionsSelected.add(true)
            else
                advanceOptionsSelected.add(false)
        }

    }

    private fun getCategory(): Int {
        var category = 0
        categorySelected.forEachIndexed { index, boolean ->
            if (boolean)
                category = category or SearchLayout.CategoryEhConfigs[index]
        }
        return category
    }

    private fun getAdvanceSearch(): Int {
        var advanceSearch = 0
        advanceOptionsSelected.forEachIndexed { index, b ->
            if (b)
                advanceSearch = advanceSearch or (1 shl index)
        }
        return advanceSearch
    }

    fun onSelectImage(event: (() -> Unit)?) {
        onSelectImage = event
    }

    @SuppressLint("NonConstantResourceId")
    @Throws(EhException::class)
    fun formatListUrlBuilder(urlBuilder: ListUrlBuilder, query: String?) {
        urlBuilder.reset()
        when (mSearchMode) {
            0 -> {
                urlBuilder.mode = ListUrlBuilder.MODE_NORMAL
                urlBuilder.keyword = query
                urlBuilder.category = getCategory()
                if (enabledAdvance) {
                    urlBuilder.advanceSearch = getAdvanceSearch()
                    urlBuilder.minRating = minRating
                    urlBuilder.pageFrom = searchPageNumber.PageFrom
                    urlBuilder.pageTo = searchPageNumber.PageTo
                }
            }
            1 -> {
                urlBuilder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                if (null == mImagePath) {
                    throw EhException(
                        EhApplication.getInstance().getString(R.string.select_image_first)
                    )
                }
                urlBuilder.imagePath = mImagePath
                urlBuilder.isUseSimilarityScan = imageSearchOptionsSelected[0]
                urlBuilder.isOnlySearchCovers = imageSearchOptionsSelected[1]
                urlBuilder.isShowExpunged = imageSearchOptionsSelected[2]
            }
        }
    }

    fun setImageUri(imageUri: Uri?) {
        if (null == imageUri) {
            return
        }
        mImageUri = imageUri
        val mContext = EhApplication.getInstance()
        val file = UniFile.fromUri(mContext, imageUri) ?: return
        try {
            val maxSize = mContext.resources.getDimensionPixelOffset(R.dimen.image_search_max_size)
            val bitmap =
                BitmapUtils.decodeStream(UniFileInputStreamPipe(file), maxSize, maxSize) ?: return
            val temp = AppConfig.createTempFile() ?: return

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


}

data class pageMunber(var PageFrom: Int, var PageTo: Int)