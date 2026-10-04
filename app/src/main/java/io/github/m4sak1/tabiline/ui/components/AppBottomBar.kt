package io.github.m4sak1.tabiline.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class AppDestination { TODAY, TIMELINE, PLANS }

@Composable
fun AppBottomBar(selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    val label: @Composable (String) -> Unit = { value -> Text(value, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold) }
    NavigationBar {
        NavigationBarItem(selected == AppDestination.TODAY, { onSelect(AppDestination.TODAY) },
            { Icon(Icons.Rounded.Today, null) }, label = { label("今日") })
        NavigationBarItem(selected == AppDestination.TIMELINE, { onSelect(AppDestination.TIMELINE) },
            { Icon(Icons.Rounded.Timeline, null) }, label = { label("タイムライン") })
        NavigationBarItem(selected == AppDestination.PLANS, { onSelect(AppDestination.PLANS) },
            { Icon(Icons.Rounded.Luggage, null) }, label = { label("旅行") })
    }
}
