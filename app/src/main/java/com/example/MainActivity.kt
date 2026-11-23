package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.PythonYolAcScreen
import com.example.ui.theme.PythonYolAcTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = uiState.darkThemeOverride ?: isSystemInDarkTheme()

            PythonYolAcTheme(
                darkTheme = isDark,
                largeText = uiState.largeTextEnabled,
            ) {
                PythonYolAcScreen(
                    uiState = uiState,
                    onSelectLevel = viewModel::selectLevel,
                    onCodeChange = viewModel::updateCode,
                    onRunCode = viewModel::runCode,
                    onStopExecution = viewModel::stopExecution,
                    onResetLevel = viewModel::resetLevel,
                    onTriggerHint = viewModel::triggerHint,
                    onCloseHint = viewModel::closeHint,
                    onNextLevel = viewModel::goToNextLevel,
                    onToggleCommandGuide = viewModel::setCommandGuideVisible,
                    onToggleLargeText = viewModel::toggleLargeText,
                    onToggleTheme = viewModel::toggleTheme,
                )
            }
        }
    }
}
