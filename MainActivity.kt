package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.EmojiCameraScreen
import com.example.ui.screens.EmojiCipherScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TranslateScreen
import com.example.ui.theme.TransMutateTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory

enum class AppScreen(val title: String) {
    TRANSLATE("Перевод"),
    HISTORY("История"),
    EMOJI_CIPHER("Эмодзи-Язык"),
    EMOJI_CAMERA("Камера")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TransMutateApp

        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(app.repository))
            val themeMode by viewModel.themeMode.collectAsState()

            var currentScreen by remember { mutableStateOf(AppScreen.TRANSLATE) }

            // BackHandler: returns to Translate screen if on other screens
            if (currentScreen != AppScreen.TRANSLATE) {
                BackHandler {
                    currentScreen = AppScreen.TRANSLATE
                }
            }

            TransMutateTheme(themeMode = themeMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.TRANSLATE,
                                onClick = { currentScreen = AppScreen.TRANSLATE },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == AppScreen.TRANSLATE) Icons.Filled.Transform else Icons.Outlined.Transform,
                                        contentDescription = "Перевод и искажение"
                                    )
                                },
                                label = { Text("Исказитель", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_item_translate")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.HISTORY,
                                onClick = { currentScreen = AppScreen.HISTORY },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == AppScreen.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                        contentDescription = "История трансформаций"
                                    )
                                },
                                label = { Text("История", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_item_history")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.EMOJI_CIPHER,
                                onClick = { currentScreen = AppScreen.EMOJI_CIPHER },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == AppScreen.EMOJI_CIPHER) Icons.Filled.EmojiEmotions else Icons.Outlined.EmojiEmotions,
                                        contentDescription = "Эмодзи-Язык"
                                    )
                                },
                                label = { Text("Эмодзи-Язык", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_item_cipher")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.EMOJI_CAMERA,
                                onClick = { currentScreen = AppScreen.EMOJI_CAMERA },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == AppScreen.EMOJI_CAMERA) Icons.Filled.PhotoCamera else Icons.Outlined.PhotoCamera,
                                        contentDescription = "Эмодзи-Камера"
                                    )
                                },
                                label = { Text("Камера", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_item_camera")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        AppScreen.TRANSLATE -> {
                            TranslateScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreen.HISTORY -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { currentScreen = AppScreen.TRANSLATE },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreen.EMOJI_CIPHER -> {
                            EmojiCipherScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppScreen.EMOJI_CAMERA -> {
                            EmojiCameraScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
