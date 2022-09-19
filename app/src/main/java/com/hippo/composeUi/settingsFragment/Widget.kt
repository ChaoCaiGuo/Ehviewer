package com.hippo.composeUi.settingsFragment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.composeUi.theme.TextColor


@Composable
fun Preference(title: String, summary: String,onClick:(()->Unit)?= null) {
    Column(modifier =Modifier.fillMaxWidth().clickable { onClick?.invoke() }.padding(horizontal = 17.dp,14.dp)) {
        Text(title, fontSize = 17.sp,color= MaterialTheme.colorScheme.TextColor,fontWeight= FontWeight.Medium)
        Text(summary, fontSize = 14.sp, color = Color.Gray,lineHeight=17.sp)
    }
}

@Composable
fun Preference(title: String, summary: AnnotatedString, onClick:(()->Unit)?= null) {
    Column(modifier =Modifier.fillMaxWidth().clickable { onClick?.invoke() }.padding(horizontal = 17.dp,12.dp)) {
        Text(title, fontSize = 17.sp,color= MaterialTheme.colorScheme.TextColor,fontWeight= FontWeight.Medium)
        Text(summary, fontSize = 14.sp, color = Color.Gray,lineHeight=17.sp, modifier = Modifier.height(60.dp))
    }
}


