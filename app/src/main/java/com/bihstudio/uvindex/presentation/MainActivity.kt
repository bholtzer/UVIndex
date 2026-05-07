package com.bihstudio.uvindex.presentation

import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.presentation.navigation.AppNavGraph
import com.bihstudio.uvindex.presentation.navigation.Screen
import com.bihstudio.uvindex.presentation.theme.NightBlue
import com.bihstudio.uvindex.presentation.theme.UVIndexTheme
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_UV_INDEX = "com.bihstudio.uvindex.OPEN_UV_INDEX"
    }

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this)

        setContent {
            val languageCode by preferencesManager.language.collectAsState(initial = "en")
            val isFirstLaunch by preferencesManager.isFirstLaunch.collectAsState(initial = null)
            val context = LocalContext.current
            
            val wrappedContext = remember(languageCode) {
                val locale = Locale(languageCode)
                val localizedConfiguration = Configuration(context.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                val localizedContext = context.createConfigurationContext(localizedConfiguration)
                object : ContextWrapper(context) {
                    override fun getResources() = localizedContext.resources
                    override fun getAssets() = localizedContext.assets
                }
            }

            CompositionLocalProvider(
                LocalContext provides wrappedContext,
                LocalConfiguration provides wrappedContext.resources.configuration
            ) {
                UVIndexTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = NightBlue
                    ) {
                        if (isFirstLaunch != null) {
                            val navController = rememberNavController()
                            AppNavGraph(
                                navController = navController,
                                startDestination = if (
                                    intent.getBooleanExtra(EXTRA_OPEN_UV_INDEX, false) ||
                                    isFirstLaunch == false
                                ) {
                                    Screen.UVIndex.route
                                } else {
                                    Screen.Splash.route
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
