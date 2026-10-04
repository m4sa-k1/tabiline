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

enum class AppDestination { TODAY, TIMELINE, PLANS }

@Composable
fun AppBottomBar(selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    NavigationBar {
        NavigationBarItem(selected == AppDestination.TODAY, { onSelect(AppDestination.TODAY) },
            { Icon(Icons.Rounded.Today, null) }, label = { Text("今日") })
        NavigationBarItem(selected == AppDestination.TIMELINE, { onSelect(AppDestination.TIMELINE) },
            { Icon(Icons.Rounded.Timeline, null) }, label = { Text("タイムライン") })
        NavigationBarItem(selected == AppDestination.PLANS, { onSelect(AppDestination.PLANS) },
            { Icon(Icons.Rounded.Luggage, null) }, label = { Text("旅行") })
    }
}
