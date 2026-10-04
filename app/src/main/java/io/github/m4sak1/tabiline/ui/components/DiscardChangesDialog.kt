package io.github.m4sak1.tabiline.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun DiscardChangesDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        icon = { Text("(｡•́︿•̀｡)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium) },
        title = { Text("入力内容を破棄しますか？") },
        text = { Text("保存していない内容は失われ、元に戻せません。閉じてもよろしいですか？") },
        confirmButton = { TextButton(onClick = onDiscard) { Text("破棄して閉じる") } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text("入力を続ける") } },
    )
}
