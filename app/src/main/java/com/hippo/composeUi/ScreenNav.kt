package com.hippo.composeUi

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hippo.ehviewer.R

sealed class ScreenNav(val route: String, @StringRes val resourceId: Int,  @DrawableRes val ImageId: Int) {
    object Home : ScreenNav("homepage", R.string.homepage, R.drawable.v_homepage_black_x24)
    object History : ScreenNav("history", R.string.history,  R.drawable.v_history_black_x24)
    object Search : ScreenNav("search", R.string.search,  R.drawable.v_magnify_x24)
    object Favourite : ScreenNav("favourite", R.string.favourite,  R.drawable.v_heart_x24)
    object Downloads : ScreenNav("downloads", R.string.downloads,  R.drawable.v_download_x24)
}