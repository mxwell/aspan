package com.khairulin.kazakhverb.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit
) {
    Column {
        Text(text = "About us")
    }
}