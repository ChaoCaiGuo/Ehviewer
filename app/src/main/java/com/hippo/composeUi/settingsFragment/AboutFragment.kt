package com.hippo.composeUi.settingsFragment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hippo.ehviewer.R
import com.hippo.composeUi.composeExt.BaseComposeFragment
import com.hippo.composeUi.theme.SystemBarsColor
import com.hippo.ehviewer.UrlOpener

const val TAG = "AboutFragment"

class AboutFragment : BaseComposeFragment(R.string.settings_about, { composeAboutFragmentUI() })


@Composable
fun composeAboutFragmentUI() {
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


    Column(Modifier.padding(vertical = 8.dp)) {
        aboutTitle.forEachIndexed { index, s ->

            val event: (() -> Unit)? = if (index in 2..3) {
                { UrlOpener.openUrl(context, aboutSummary[index], true) }
            } else
                null

            if (index == 1)
                Preference(title = s, summary = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.W900,
                        textDecoration = TextDecoration.LineThrough)
                    ) {
                        append(("ehviewersu!gmail.com\n".replace('!', '@')))
                    }
                    append("NekoInverter\n")
                    append("飛鳥澪 \n")
                }, null)
            else
                Preference(title = s, summary = aboutSummary[index], event)
        }
    }
}


