package com.khairulin.kazakhverb

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.khairulin.kazakhverb.view.AboutScreen
import kotlinx.serialization.Serializable

@Serializable
object MainTabViewTag
@Serializable
object AboutScreenTag

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainTabViewTag
    ) {
        composable<MainTabViewTag> {
            MainTabView(navController)
        }

        composable<AboutScreenTag> {
            AboutScreen()
        }
    }
}