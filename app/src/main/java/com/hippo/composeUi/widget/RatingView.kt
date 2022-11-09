package com.hippo.composeUi.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.hippo.ehviewer.R

@Composable
fun RatingView(rating: Float = 0f) {
    val context = LocalContext.current
    when (rating) {
        in (0f..5f) -> {
            Row {
                (0 until rating.toInt()).forEach { _ ->
                    AsyncImage(
                        model = ContextCompat.getDrawable(context, R.drawable.v_star_x16),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(16.dp)
                    )
                }
                if (rating % 1 != 0f)
                    AsyncImage(
                        model = ContextCompat.getDrawable(context, R.drawable.v_star_half_x16),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(16.dp)
                    )
                (0 until (5f - rating).toInt()).forEach { _ ->
                    AsyncImage(
                        model = ContextCompat.getDrawable(context, R.drawable.v_star_outline_x16),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(16.dp)
                    )
                }
            }
        }
        else -> {
            Row{
                (0 until 5).forEach { _ ->

                    AsyncImage(
                        model = ContextCompat.getDrawable(context, R.drawable.v_star_outline_x16),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(16.dp)
                    )
                }
            }

        }
    }

}