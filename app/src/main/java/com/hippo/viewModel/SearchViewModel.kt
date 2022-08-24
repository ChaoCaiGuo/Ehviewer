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
    val category_selected = mutableStateListOf<Boolean>()
    val advance_selected  = mutableStateListOf<Boolean>()
    val image_selected  = mutableStateListOf<Boolean>()

    var advance_minRating = -1
    var advance_pageMunber =pageMunber(-1,-1)
    var image_path by mutableStateOf<Uri?>(null)
    var enabledAdvance by mutableStateOf(false)
    val verticalScroll = ScrollState(initial = 0)
    var mSearchMode = 0


    init{
        if(image_selected.size == 0) {
            image_selected.add(true)
            image_selected.add(false)
            image_selected.add(false)
        }
        if(category_selected.size==0) {
            repeat(10){
                category_selected.add(true)
            }
        }
        if(advance_selected.size == 0) {
            (0 until EhApplication.getInstance().resources.getStringArray(R.array.AdvanceSearch).size).forEach {
                if(it<2)
                    advance_selected.add(true)
                else
                    advance_selected.add(false)
            }
        }
    }
    fun getCategory(): Int {
        var category = 0
        category_selected.forEachIndexed { index, boolean ->
            if (boolean)
                category = category or SearchLayout.EhConfigs[index]
        }
        return category
    }

    fun getAdvanceSearch(): Int {
        var advanceSearch = 0
        advance_selected.forEachIndexed { index, b ->
            if (b)
                advanceSearch = advanceSearch or (1 shl index)
        }
        return advanceSearch
    }

}

data class pageMunber(var PageFrom:Int, var PageTo:Int)