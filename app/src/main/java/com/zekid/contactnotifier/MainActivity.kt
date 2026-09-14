package com.zekid.contactnotifier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.zekid.contactnotifier.data.SettingsRepository
import com.zekid.contactnotifier.ui.navigation.NavRoute
import com.zekid.contactnotifier.ui.settings.SettingsScreen
import com.zekid.contactnotifier.ui.settings.SettingsViewModel
import com.zekid.contactnotifier.ui.settings.SettingsViewModelFactory
import com.zekid.contactnotifier.ui.theme.NtfyFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            NtfyFlowTheme {
                val backStack = rememberNavBackStack(NavRoute.Settings)
                
                val provider: (NavKey) -> NavEntry<NavKey> = entryProvider {
                    entry<NavRoute.Settings> {
                        val viewModel: SettingsViewModel = viewModel(
                            factory = SettingsViewModelFactory(settingsRepository)
                        )
                        SettingsScreen(viewModel = viewModel)
                    }
                }

                val decorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>()
                )
                val entries = rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryDecorators = decorators,
                    entryProvider = provider
                )

                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    entries = entries.toMutableStateList(),
                    onBack = { 
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        } else {
                            finish()
                        }
                    }
                )
            }
        }
    }
}
