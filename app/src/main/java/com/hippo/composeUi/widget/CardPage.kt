package com.hippo.composeUi.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CardPage(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.padding(4.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Box(modifier = Modifier.padding(15.dp)) {
            content.invoke()
        }
    }
}