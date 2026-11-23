package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ExecutionUiState
import com.example.model.PuzzleLevel
import com.example.ui.components.CodeEditorPanel
import com.example.ui.components.CommandGuideDialog
import com.example.ui.components.GameBoardSection
import com.example.ui.components.accessibleFocusRing
import com.example.ui.theme.EditorBackground
import com.example.ui.theme.EditorText
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.HintContainerDark
import com.example.ui.theme.HintContainerLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OnErrorContainerDark
import com.example.ui.theme.OnErrorContainerLight
import com.example.ui.theme.OnHintContainerDark
import com.example.ui.theme.OnHintContainerLight
import com.example.ui.theme.OnSuccessContainerDark
import com.example.ui.theme.OnSuccessContainerLight
import com.example.ui.theme.SuccessContainerDark
import com.example.ui.theme.SuccessContainerLight
import com.example.viewmodel.GameUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PythonYolAcScreen(
    uiState: GameUiState,
    onSelectLevel: (Int) -> Unit,
    onCodeChange: (String) -> Unit,
    onRunCode: () -> Unit,
    onStopExecution: () -> Unit,
    onResetLevel: () -> Unit,
    onTriggerHint: () -> Unit,
    onCloseHint: () -> Unit,
    onNextLevel: () -> Unit,
    onToggleCommandGuide: (Boolean) -> Unit,
    onToggleLargeText: () -> Unit,
    onToggleTheme: (Boolean) -> Unit,
) {
    val isDark = uiState.darkThemeOverride ?: isSystemInDarkTheme()
    val editorFocusRequester = remember { FocusRequester() }
    val runButtonFocusRequester = remember { FocusRequester() }

    if (uiState.isCommandGuideVisible) {
        CommandGuideDialog(
            commands = uiState.currentLevel.commandDocs,
            darkThemeOverride = uiState.darkThemeOverride,
            onDismiss = { onToggleCommandGuide(false) },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = "Rahat Mod • ${uiState.completedLevelIds.size}/${uiState.levels.size} bölüm",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onToggleCommandGuide(true) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = uiState.darkThemeOverride)
                            .testTag("command_guide_button")
                            .semantics { contentDescription = "Komut rehberini aç" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                        )
                    }
                    IconButton(
                        onClick = onToggleLargeText,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = uiState.darkThemeOverride)
                            .testTag("large_text_toggle_button")
                            .semantics {
                                contentDescription = if (uiState.largeTextEnabled) {
                                    "Büyük yazı modu açık, kapatmak için dokunun"
                                } else {
                                    "Büyük yazı modunu aç"
                                }
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = null,
                            tint = if (uiState.largeTextEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = { onToggleTheme(isDark) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = uiState.darkThemeOverride)
                            .testTag("theme_toggle_button")
                            .semantics {
                                contentDescription = if (isDark) "Açık temaya geç" else "Koyu temaya geç"
                            },
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val isWide = maxWidth >= 840.dp
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Screen-reader status announcer for level switches, resets, and theme toggles
                if (uiState.statusAnnouncement != null) {
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = uiState.statusAnnouncement
                            },
                    )
                }

                // Level selector bar
                LevelSelectorBar(
                    levels = uiState.levels,
                    currentLevelId = uiState.currentLevel.id,
                    completedIds = uiState.completedLevelIds,
                    darkThemeOverride = uiState.darkThemeOverride,
                    onSelectLevel = onSelectLevel,
                    modifier = Modifier.widthIn(max = 1280.dp),
                )

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 1280.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            LessonAndTaskCard(
                                level = uiState.currentLevel,
                                isCompleted = uiState.isCurrentLevelCompleted,
                                darkThemeOverride = uiState.darkThemeOverride,
                                onOpenGuide = { onToggleCommandGuide(true) },
                            )
                            GameBoardSection(
                                board = uiState.board,
                                lastStepMessage = uiState.lastStepMessage,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CodeEditorPanel(
                                code = uiState.code,
                                activeLine = uiState.activeMoveLine,
                                largeText = uiState.largeTextEnabled,
                                darkThemeOverride = uiState.darkThemeOverride,
                                editorFocusRequester = editorFocusRequester,
                                runButtonFocusRequester = runButtonFocusRequester,
                                onCodeChange = onCodeChange,
                                onRunRequested = onRunCode,
                            )
                            ActionButtonsRow(
                                uiState = uiState,
                                darkThemeOverride = uiState.darkThemeOverride,
                                runButtonFocusRequester = runButtonFocusRequester,
                                onRunCode = onRunCode,
                                onStopExecution = onStopExecution,
                                onResetLevel = onResetLevel,
                                onTriggerHint = onTriggerHint,
                                onNextLevel = onNextLevel,
                            )
                            HintSectionCard(
                                uiState = uiState,
                                isDark = isDark,
                                darkThemeOverride = uiState.darkThemeOverride,
                                onShowMoreHint = onTriggerHint,
                                onCloseHint = onCloseHint,
                            )
                            FeedbackResultCard(
                                executionState = uiState.executionState,
                                isDark = isDark,
                                allLevelsCompleted = uiState.completedLevelIds.size == uiState.levels.size,
                                hasNextLevel = uiState.hasNextLevel,
                                darkThemeOverride = uiState.darkThemeOverride,
                                onNextLevel = onNextLevel,
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LessonAndTaskCard(
                            level = uiState.currentLevel,
                            isCompleted = uiState.isCurrentLevelCompleted,
                            darkThemeOverride = uiState.darkThemeOverride,
                            onOpenGuide = { onToggleCommandGuide(true) },
                        )
                        GameBoardSection(
                            board = uiState.board,
                            lastStepMessage = uiState.lastStepMessage,
                        )
                        CodeEditorPanel(
                            code = uiState.code,
                            activeLine = uiState.activeMoveLine,
                            largeText = uiState.largeTextEnabled,
                            darkThemeOverride = uiState.darkThemeOverride,
                            editorFocusRequester = editorFocusRequester,
                            runButtonFocusRequester = runButtonFocusRequester,
                            onCodeChange = onCodeChange,
                            onRunRequested = onRunCode,
                        )
                        ActionButtonsRow(
                            uiState = uiState,
                            darkThemeOverride = uiState.darkThemeOverride,
                            runButtonFocusRequester = runButtonFocusRequester,
                            onRunCode = onRunCode,
                            onStopExecution = onStopExecution,
                            onResetLevel = onResetLevel,
                            onTriggerHint = onTriggerHint,
                            onNextLevel = onNextLevel,
                        )
                        HintSectionCard(
                            uiState = uiState,
                            isDark = isDark,
                            darkThemeOverride = uiState.darkThemeOverride,
                            onShowMoreHint = onTriggerHint,
                            onCloseHint = onCloseHint,
                        )
                        FeedbackResultCard(
                            executionState = uiState.executionState,
                            isDark = isDark,
                            allLevelsCompleted = uiState.completedLevelIds.size == uiState.levels.size,
                            hasNextLevel = uiState.hasNextLevel,
                            darkThemeOverride = uiState.darkThemeOverride,
                            onNextLevel = onNextLevel,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelSelectorBar(
    levels: List<PuzzleLevel>,
    currentLevelId: Int,
    completedIds: Set<Int>,
    darkThemeOverride: Boolean?,
    onSelectLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        levels.forEach { level ->
            val isSelected = level.id == currentLevelId
            val isCompleted = level.id in completedIds
            val stateLabel = when {
                isSelected && isCompleted -> "Seçili ve tamamlandı"
                isSelected -> "Seçili"
                isCompleted -> "Tamamlandı"
                else -> "Oynanabilir"
            }

            if (isSelected) {
                Button(
                    onClick = { onSelectLevel(level.id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = darkThemeOverride)
                        .testTag("level_tab_${level.id}")
                        .semantics {
                            selected = true
                            stateDescription = stateLabel
                            contentDescription = "${level.title}, $stateLabel"
                        },
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onSelectLevel(level.id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = darkThemeOverride)
                        .testTag("level_tab_${level.id}")
                        .semantics {
                            selected = false
                            stateDescription = stateLabel
                            contentDescription = "${level.title}, $stateLabel"
                        },
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonAndTaskCard(
    level: PuzzleLevel,
    isCompleted: Boolean,
    darkThemeOverride: Boolean?,
    onOpenGuide: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = level.conceptTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                if (isCompleted) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Tamamlandı",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Text(
                text = level.conceptExplanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
            ) {
                Text(
                    text = level.taskDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Örnek: hareket_et(\"kirmizi\", \"sag\", 2)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onOpenGuide,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .accessibleFocusRing(RoundedCornerShape(10.dp), darkThemeOverride = darkThemeOverride)
                        .testTag("inline_command_guide_button")
                        .semantics { contentDescription = "Tüm Python komutlarının açıklamasını aç" },
                ) {
                    Text("Komutları Gör")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionButtonsRow(
    uiState: GameUiState,
    darkThemeOverride: Boolean?,
    runButtonFocusRequester: FocusRequester,
    onRunCode: () -> Unit,
    onStopExecution: () -> Unit,
    onResetLevel: () -> Unit,
    onTriggerHint: () -> Unit,
    onNextLevel: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (uiState.isRunning) {
            Button(
                onClick = onStopExecution,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .focusRequester(runButtonFocusRequester)
                    .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = darkThemeOverride)
                    .testTag("stop_code_button")
                    .semantics { contentDescription = "Çalışan Python kodunu durdur" },
            ) {
                Icon(imageVector = Icons.Filled.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.stop_execution),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Button(
                onClick = onRunCode,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .focusRequester(runButtonFocusRequester)
                    .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = darkThemeOverride)
                    .testTag("run_code_button")
                    .semantics { contentDescription = "Yazılan Python kodunu çalıştır" },
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.run_code),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        OutlinedButton(
            onClick = onResetLevel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = darkThemeOverride)
                .testTag("reset_level_button")
                .semantics { contentDescription = "Bölümü ve başlangıç kodunu sıfırla" },
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.reset_level),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        FilledTonalButton(
            onClick = onTriggerHint,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = darkThemeOverride)
                .testTag("hint_button")
                .semantics { contentDescription = "Bu bölüm için ipucu göster" },
        ) {
            Icon(imageVector = Icons.Filled.Lightbulb, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.hint),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        if (uiState.isCurrentLevelCompleted && uiState.hasNextLevel) {
            Button(
                onClick = onNextLevel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = darkThemeOverride)
                    .testTag("next_level_button")
                    .semantics { contentDescription = "Sonraki bölüme geç" },
            ) {
                Text(
                    text = stringResource(R.string.next_level),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun HintSectionCard(
    uiState: GameUiState,
    isDark: Boolean,
    darkThemeOverride: Boolean?,
    onShowMoreHint: () -> Unit,
    onCloseHint: () -> Unit,
) {
    AnimatedVisibility(visible = uiState.isHintVisible && uiState.revealedHintCount > 0) {
        val bg = if (isDark) HintContainerDark else HintContainerLight
        val fg = if (isDark) OnHintContainerDark else OnHintContainerLight
        val hintsToShow = uiState.currentLevel.hints.take(uiState.revealedHintCount)
        val hasMore = uiState.revealedHintCount < uiState.currentLevel.hints.size

        Card(
            colors = CardDefaults.cardColors(containerColor = bg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, fg.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .testTag("hint_card"),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.Lightbulb, contentDescription = null, tint = fg)
                        Text(
                            text = "İpucu (${uiState.revealedHintCount}/${uiState.currentLevel.hints.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = fg,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    TextButton(
                        onClick = onCloseHint,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .accessibleFocusRing(RoundedCornerShape(10.dp), darkThemeOverride = darkThemeOverride)
                            .semantics { contentDescription = "İpucu panelini gizle" },
                    ) {
                        Text("Gizle", color = fg, fontWeight = FontWeight.Bold)
                    }
                }

                hintsToShow.forEach { hintText ->
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = fg,
                    )
                }

                if (hasMore) {
                    OutlinedButton(
                        onClick = onShowMoreHint,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .accessibleFocusRing(RoundedCornerShape(10.dp), darkThemeOverride = darkThemeOverride)
                            .testTag("more_hint_button"),
                    ) {
                        Text("Bir ipucu daha göster", color = fg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackResultCard(
    executionState: ExecutionUiState,
    isDark: Boolean,
    allLevelsCompleted: Boolean,
    hasNextLevel: Boolean,
    darkThemeOverride: Boolean?,
    onNextLevel: () -> Unit,
) {
    val liveMode = when (executionState) {
        is ExecutionUiState.Error, is ExecutionUiState.Solved, is ExecutionUiState.Incomplete -> LiveRegionMode.Assertive
        else -> LiveRegionMode.Polite
    }

    val (containerColor, contentColor) = when (executionState) {
        is ExecutionUiState.Solved -> {
            (if (isDark) SuccessContainerDark else SuccessContainerLight) to
                (if (isDark) OnSuccessContainerDark else OnSuccessContainerLight)
        }
        is ExecutionUiState.Error -> {
            (if (isDark) ErrorContainerDark else ErrorContainerLight) to
                (if (isDark) OnErrorContainerDark else OnErrorContainerLight)
        }
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, contentColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .testTag("feedback_panel")
            .semantics { liveRegion = liveMode },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Sonuç ve Geri Bildirim",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.semantics { heading() },
            )

            when (executionState) {
                is ExecutionUiState.Idle -> {
                    Text(
                        text = "Hazır. Kodunu düzenle ve \"Kodu çalıştır\" düğmesine bas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is ExecutionUiState.Running -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Text(
                            text = executionState.statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor,
                        )
                    }
                }
                is ExecutionUiState.Solved -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = contentColor,
                        )
                        Text(
                            text = "Bölüm Tamamlandı!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                        )
                    }
                    Text(
                        text = executionState.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor,
                    )
                    if (allLevelsCompleted && !hasNextLevel) {
                        Text(
                            text = "Harika iş! İlk aşamadaki tüm bölümleri (Değişkenler, Koşullar ve Döngüler) başarıyla tamamladın.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                        )
                    } else if (hasNextLevel) {
                        Button(
                            onClick = onNextLevel,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = darkThemeOverride)
                                .testTag("feedback_next_level_button"),
                        ) {
                            Text(stringResource(R.string.next_level))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                    ConsoleOutputBox(executionState.consoleOutput)
                }
                is ExecutionUiState.Incomplete -> {
                    Text(
                        text = executionState.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor,
                    )
                    ConsoleOutputBox(executionState.consoleOutput)
                }
                is ExecutionUiState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = contentColor,
                        )
                        Text(
                            text = executionState.errorTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                        )
                    }
                    Text(
                        text = executionState.turkishExplanation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor,
                    )
                    ConsoleOutputBox(executionState.consoleOutput)
                }
            }
        }
    }
}

@Composable
private fun ConsoleOutputBox(lines: List<String>) {
    if (lines.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(EditorBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "print() Çıktısı:",
            style = MaterialTheme.typography.labelMedium,
            color = EditorText.copy(alpha = 0.75f),
            fontFamily = JetBrainsMonoFontFamily,
        )
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = EditorText,
                fontFamily = JetBrainsMonoFontFamily,
            )
        }
    }
}
