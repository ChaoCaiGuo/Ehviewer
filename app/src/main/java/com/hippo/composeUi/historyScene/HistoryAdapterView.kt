package com.hippo.composeUi.historyScene

import android.text.TextUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import com.google.accompanist.flowlayout.FlowColumn
import com.hippo.composeUi.theme.EhViewerTheme
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings.KEY_LIST_THUMB_SIZE
import com.hippo.ehviewer.Settings.getInt
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.widget.SimpleRatingView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryAdapterView(
    gi: GalleryInfo,
    isShowDownloaded: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {

    val ratio = 3
    EhViewerTheme {

        val listCardSize = remember { getInt(KEY_LIST_THUMB_SIZE, 40) }
        val maxLines = remember { derivedStateOf { if (listCardSize >= 35) 2 else 1 } }

        Card(
            Modifier
                .combinedClickable(
                    onLongClick = onItemLongClick,
                    onClick = onItemClick
                )
                .padding(vertical = 3.dp, horizontal = 3.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Row(modifier = Modifier.height((listCardSize * ratio).dp)) {
                //这儿是封面图片
                SubcomposeAsyncImage(
                    model = gi.thumb,
                    contentDescription = "",
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .width((listCardSize * ratio * (2 / 3f)).dp)
                        .height((listCardSize * ratio).dp)
                )

                Column(Modifier.padding(4.dp, 5.dp)) {
                    //这儿是标题
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(fontWeight = FontWeight.W600)
                            ) {
                                append(EhUtils.getSuitableTitle(gi))
                            }
                        }, maxLines = maxLines.value,
                        modifier = Modifier.fillMaxWidth(),
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        FlowColumn {
                            //这儿是上传者
                            Text(
                                text = gi.uploader ?: "(DISOWNED)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W500

                            )
                            //这儿是评分
                            AndroidView(factory = { context ->
                                SimpleRatingView(context).also {
                                    it.rating = gi.rating
                                }
                            })
                            //这儿是分类
                            Text(
                                text = EhUtils.getCategory(gi.category),
                                modifier = Modifier
                                    .padding(2.dp)
                                    .background(Color(EhUtils.getCategoryColor(gi.category)))
                                    .padding(vertical = 3.dp, horizontal = 4.dp),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.W600
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                //这儿是下载图标
                                if (isShowDownloaded) {
                                    Icon(
                                        painterResource(id = R.drawable.v_download_x16),
                                        contentDescription = null
                                    )
                                }

                                //这儿是语言
                                Text(
                                    text = if (TextUtils.isEmpty(gi.simpleLanguage)) "" else gi.simpleLanguage,
                                    fontSize = 14.sp
                                )
                            }
                            //这是上传时间
                            Text(text = gi.posted, fontSize = 14.sp)
                        }

                    }


                }
            }
        }

    }
}