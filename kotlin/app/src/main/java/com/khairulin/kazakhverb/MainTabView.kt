package com.khairulin.kazakhverb

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.khairulin.kazakhverb.view.ConjugationScreen
import com.khairulin.kazakhverb.view.SettingsScreen
import com.khairulin.kazakhverb.vm.ConjugationVM

@Composable
fun MainTabView(
    navController: NavController
) {
    val conjugationVM: ConjugationVM = viewModel()

    val tabs = listOf("Conjugation", "Settings")

    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (index == 0) {
                                    Icons.Filled.Menu
                                } else {
                                    Icons.Filled.Settings
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTabIndex) {
                0 -> ConjugationScreen(conjugationVM = conjugationVM)
                1 -> SettingsScreen(conjugationVM = conjugationVM) {
                    navController.navigate(AboutScreenTag)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomTabLayoutPreview() {
    MaterialTheme {
        MainTabView(rememberNavController())
    }
}