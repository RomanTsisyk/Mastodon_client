package io.github.romantsisyk.mastodon.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.romantsisyk.mastodon.data.preferences.AppPreferencesManager
import io.github.romantsisyk.mastodon.ui.screens.TimelineScreen
import io.github.romantsisyk.mastodon.ui.screens.WelcomeScreen
import io.github.romantsisyk.mastodon.ui.theme.MastodonTheme
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: AppPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Timber.d("Starting app - first launch: ${preferencesManager.isFirstLaunch()}")
        setContent {
            MastodonApp(preferencesManager)
        }
    }
}

@Composable
fun MastodonApp(preferencesManager: AppPreferencesManager) {
    MastodonTheme {
        var showWelcome by remember { mutableStateOf(preferencesManager.isFirstLaunch()) }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            if (showWelcome) {
                WelcomeScreen(
                    onGetStarted = {
                        preferencesManager.markFirstLaunchComplete()
                        showWelcome = false
                        Timber.d("Welcome screen completed, showing timeline")
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                TimelineScreen()
                Timber.d("Timeline screen is showing")
            }
        }
    }
}