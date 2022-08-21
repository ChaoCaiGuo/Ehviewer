package com.hippo.viewModel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SearchViewModel:ViewModel(){
    val image_selected  = mutableStateListOf<Boolean>()
    var image_path by mutableStateOf<Uri?>(null)

    init{
        if(image_selected.size == 0) {
            image_selected.add(true)
            image_selected.add(false)
            image_selected.add(false)
        }
    }







}

