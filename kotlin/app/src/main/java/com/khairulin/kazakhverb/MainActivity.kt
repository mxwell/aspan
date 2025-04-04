package com.khairulin.kazakhverb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.khairulin.kazakhverb.config.SharedPreferencesManager
import com.khairulin.kazakhverb.ui.theme.KazakhVerbTheme
import com.khairulin.kazakhverb.verbdb.TrieLoader

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SharedPreferencesManager.init(this)
        TrieLoader.loadTrie(this)

        setContent {
            KazakhVerbTheme {
                AppNavigation()
            }
        }
    }
}
