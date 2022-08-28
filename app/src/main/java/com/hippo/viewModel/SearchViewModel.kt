package com.hippo.viewModel

import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.hippo.composeUi.searchLayout.SearchLayout
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R

class SearchViewModel:ViewModel(){
    val categorySelected = mutableStateListOf<Boolean>()
    val advanceOptionsSelected  = mutableStateListOf<Boolean>()
    val imageSearchOptionsSelected  = mutableStateListOf<Boolean>()

    var minRating = -1
    var searchPageNumber =pageMunber(-1,-1)
    var imageUri by mutableStateOf<Uri?>(null)
    var enabledAdvance by mutableStateOf(false)
    val verticalScroll = ScrollState(initial = 0)
    var mSearchMode = 0


    init{
        if(imageSearchOptionsSelected.size == 0) {
            imageSearchOptionsSelected.add(true)
            imageSearchOptionsSelected.add(false)
            imageSearchOptionsSelected.add(false)
        }
        if(categorySelected.size==0) {
            repeat(10){
                categorySelected.add(true)
            }
        }
        if(advanceOptionsSelected.size == 0) {
            (0 until EhApplication.getInstance().resources.getStringArray(R.array.AdvanceSearchOptions).size).forEach {
                if(it<2)
                    advanceOptionsSelected.add(true)
                else
                    advanceOptionsSelected.add(false)
            }
        }
    }

    fun getCategory(): Int {
        var category = 0
        categorySelected.forEachIndexed { index, boolean ->
            if (boolean)
                category = category or SearchLayout.CategoryEhConfigs[index]
        }
        return category
    }

    fun getAdvanceSearch(): Int {
        var advanceSearch = 0
        advanceOptionsSelected.forEachIndexed { index, b ->
            if (b)
                advanceSearch = advanceSearch or (1 shl index)
        }
        return advanceSearch
    }

}

data class pageMunber(var PageFrom:Int, var PageTo:Int)