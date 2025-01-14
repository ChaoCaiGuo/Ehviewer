package com.hippo.composeUi.settingsFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hippo.composeUi.theme.EhViewerTheme
import com.hippo.composeUi.theme.SystemBarsColor
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.UrlOpener

const val TAG = "AboutFragment"

class AboutFragment : Fragment(){

    override fun onStart() {
        super.onStart()
        requireActivity().setTitle(R.string.settings_about)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
            .apply {
                setContent { EhViewerTheme { ComposeAboutFragmentUI() } }
            }
    }

    @Composable
    fun ComposeAboutFragmentUI() {
        val systemUiController = rememberSystemUiController()
        val systemBarsColor =MaterialTheme.colorScheme.SystemBarsColor
        SideEffect {

            systemUiController.setSystemBarsColor(
                color = systemBarsColor,
            )
        }

        val aboutTitle = stringArrayResource(id = R.array.about_title)
        val aboutSummary = stringArrayResource(id = R.array.about_summary)
        val context = LocalContext.current
        aboutSummary[4] = Settings.getVersionName().toString()
        Column(Modifier.padding(top= 6.dp)) {
            aboutTitle.forEachIndexed { index, s ->

                val event: (() -> Unit)? = if (index in 2..3) {
                    { UrlOpener.openUrl(context, aboutSummary[index], true) }
                } else
                    null

                if (index == 1)
                    Preference(title = s, summary = buildAnnotatedString {
                        append("NekoInverter\n")
                        append("飛鳥澪 \n")
                        append("Compose版 by Sin0 \n")
                    }, null)
                else
                    Preference(title = s, summary = aboutSummary[index], event)
            }
        }
    }


}




