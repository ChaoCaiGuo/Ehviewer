package com.hippo.composeUi.signInScene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.composeUi.theme.EhViewerTheme
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.scene.SolidScene


class WarningScene : SolidScene() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return (inflater.inflate(R.layout.compose_xml, container, false) as ComposeView)
            .apply {
                setContent { EhViewerTheme { ComposeWarningScene() } }
            }
    }

    @Composable
    private fun ComposeWarningScene() {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.v_alert_red_x48),
                contentDescription = null
            )
            Text(
                text = stringResource(id = R.string.app_waring),
                modifier = Modifier
                    .padding(32.dp)
                    .weight(1f),
                fontSize = 16.sp
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp, start = 5.dp, end = 5.dp)
            ) {
                OutlinedButton(onClick = { finish() }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.reject))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {

                    Settings.putShowWarning(false)
                    val activity = mainActivity
                    if (null != activity) {
                        startSceneForCheckStep(CHECK_STEP_WARNING, arguments)
                    }
                    finish()

                }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.accept))
                }
            }

        }

    }
}

