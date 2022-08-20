package com.hippo.composeUi.composeExt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.hippo.composeUi.theme.EhViewerTheme

/**
 * Fragment添加Compose的方式
 * 如果闪退的，请重新rebuild project
 */
open class BaseComposeFragment(@StringRes val title: Int,
                               private val composeContent: @Composable () -> Unit ): Fragment()  {

    override fun onStart() {
        super.onStart()
        requireActivity().setTitle(title)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LinearLayout(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view as ViewGroup).addView(
            ComposeView(requireContext())
                .apply { setContent {EhViewerTheme{composeContent.invoke()}  } })

    }

}