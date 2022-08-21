package com.hippo.composeUi.settingsFragment

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hippo.composeUi.composeExt.clickable2
import com.hippo.composeUi.theme.TextColor

@Composable
fun AdvanceSearchItem(text: String, checked: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable2(enabledIndication = false){onClick.invoke()}) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onClick.invoke() },
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5E35B1))
        )
        Text(text = text, color = MaterialTheme.colorScheme.TextColor)

    }
}