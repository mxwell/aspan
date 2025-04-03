package com.khairulin.kazakhverb.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khairulin.kazakhverb.vm.ConjugationVM

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    vm: ConjugationVM = viewModel()
) {
    val tensesConfig by vm.tenseConfigFlow.collectAsState()
    val formConfig by vm.formConfigFlow.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        item {
            Text(
                text = "Tenses and moods",
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp, start = 10.dp, end = 10.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        itemsIndexed(tensesConfig.settings) { index, setting ->
            SettingItem(title = setting.title, isChecked = setting.on) {
                vm.toggleTenseSetting(index)
            }

            if (index < tensesConfig.settings.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        item {
            Text(
                text = "Grammar forms",
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp, start = 10.dp, end = 10.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        itemsIndexed(formConfig.settings) { index, setting ->
            SettingItem(title = setting.title, isChecked = setting.on) {
                vm.toggleFormSetting(index)
            }

            if (index < formConfig.settings.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
        // Add your profile content here
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}