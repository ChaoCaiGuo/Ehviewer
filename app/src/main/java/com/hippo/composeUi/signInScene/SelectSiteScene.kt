package com.hippo.composeUi.signInScene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.hippo.composeUi.composeExt.clickable2
import com.hippo.composeUi.theme.EhViewerTheme
import com.hippo.composeUi.theme.SystemBarsColor
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.scene.SolidScene

class SelectSiteScene : SolidScene() {

    override fun needShowLeftDrawer(): Boolean {
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return (inflater.inflate(R.layout.compose_xml, container, false) as ComposeView)
            .apply {
                setContent { EhViewerTheme { ComposeSelectSiteScene() } }
            }
    }


    @Preview
    @Composable
    fun ComposeSelectSiteScene() {
        val systemUiController = rememberSystemUiController()
        val systemBarsColor = MaterialTheme.colorScheme.SystemBarsColor
        SideEffect {

            systemUiController.setSystemBarsColor(
                color = systemBarsColor,
            )
        }


        var selected by remember {
            mutableStateOf(0)
        }

        Column(
            Modifier
                .padding(vertical = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(id = R.string.select_scene), fontSize = 20.sp)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .border(1.dp, Color(0xFF575656), RoundedCornerShape(90f, 90f, 90f, 90f))
                    .clip(RoundedCornerShape(90f, 90f, 90f, 90f))
            ) {
                Box(
                    modifier = Modifier
                        .background(if (selected == 0) Color(0xFFB6ADCA) else Color.White)
                        .clickable2 { selected = 0 }
                        .size(100.dp, 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(id = R.string.site_e), fontSize = 14.sp)
                }

                Divider(
                    color = Color.Black, modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )

                Box(
                    modifier = Modifier
                        .background(if (selected == 1) Color(0xFFB6ADCA) else Color.White)
                        .clickable2 { selected = 1 }
                        .size(100.dp, 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(id = R.string.site_ex), fontSize = 14.sp)
                }
            }
            Text(
                text = stringResource(id = R.string.select_scene_explain),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 20.dp)
                    .fillMaxWidth()
            ) {
                Button(onClick = {
                    Settings.putSelectSite(false)
                    if (selected == 0)
                        Settings.putGallerySite(EhUrl.SITE_E)
                    else
                        Settings.putGallerySite(EhUrl.SITE_EX)

                    startSceneForCheckStep(CHECK_STEP_SELECT_SITE, arguments)
                    finish()
                }, Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            }
        }


    }
}

