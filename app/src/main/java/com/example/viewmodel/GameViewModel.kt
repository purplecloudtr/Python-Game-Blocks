package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LevelCatalog
import com.example.data.ProgressRepository
import com.example.model.BoardState
import com.example.model.ExecutionUiState
import com.example.model.PuzzleLevel
import com.example.python.PythonEvaluator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
    val levels: List<PuzzleLevel> = LevelCatalog.levels,
    val currentLevel: PuzzleLevel = LevelCatalog.levels.first(),
    val code: String = LevelCatalog.levels.first().starterCode,
    val board: BoardState = LevelCatalog.levels.first().initialBoard,
    val executionState: ExecutionUiState = ExecutionUiState.Idle,
    val completedLevelIds: Set<Int> = emptySet(),
    val revealedHintCount: Int = 0,
    val isHintVisible: Boolean = false,
    val isCommandGuideVisible: Boolean = false,
    val largeTextEnabled: Boolean = false,
    val darkThemeOverride: Boolean? = null,
    val lastStepMessage: String? = null,
    val activeMoveLine: Int? = null,
    val statusAnnouncement: String? = null,
) {
    val isCurrentLevelCompleted: Boolean
        get() = currentLevel.id in completedLevelIds

    val hasNextLevel: Boolean
        get() = levels.any { it.id == currentLevel.id + 1 }

    val isRunning: Boolean
        get() = executionState is ExecutionUiState.Running
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProgressRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null
    private var saveCodeJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = repository.progressFlow.first()
            val level = LevelCatalog.getLevelById(saved.currentLevelId)
            val initialCode = saved.savedCodeByLevel[level.id] ?: level.starterCode
            _uiState.update {
                it.copy(
                    currentLevel = level,
                    code = initialCode,
                    board = level.initialBoard,
                    completedLevelIds = saved.completedLevelIds,
                    largeTextEnabled = saved.largeTextEnabled,
                )
            }
        }
    }

    fun selectLevel(levelId: Int) {
        cancelExecutionInternal()
        viewModelScope.launch {
            val saved = repository.progressFlow.first()
            val level = LevelCatalog.getLevelById(levelId)
            val savedCode = saved.savedCodeByLevel[level.id] ?: level.starterCode
            repository.setCurrentLevel(level.id)
            _uiState.update {
                it.copy(
                    currentLevel = level,
                    code = savedCode,
                    board = level.initialBoard,
                    executionState = ExecutionUiState.Idle,
                    revealedHintCount = 0,
                    isHintVisible = false,
                    lastStepMessage = null,
                    activeMoveLine = null,
                    statusAnnouncement = "${level.title} açıldı. ${level.taskDescription}",
                )
            }
        }
    }

    fun updateCode(newCode: String) {
        val levelId = _uiState.value.currentLevel.id
        _uiState.update { it.copy(code = newCode) }
        saveCodeJob?.cancel()
        saveCodeJob = viewModelScope.launch {
            delay(350)
            repository.saveCode(levelId, newCode)
        }
    }

    fun runCode() {
        val snapshot = _uiState.value
        val level = snapshot.currentLevel
        val sourceCode = snapshot.code

        cancelExecutionInternal()
        saveCodeJob?.cancel()

        executionJob = viewModelScope.launch {
            repository.saveCode(level.id, sourceCode)
            _uiState.update {
                it.copy(
                    board = level.initialBoard,
                    executionState = ExecutionUiState.Running("Python kodu çalıştırılıyor..."),
                    lastStepMessage = null,
                    activeMoveLine = null,
                    statusAnnouncement = "Python kodu çalıştırılıyor.",
                )
            }

            try {
                val output = withContext(Dispatchers.Default) {
                    PythonEvaluator(
                        initialBoard = level.initialBoard,
                        maxSteps = 50_000,
                        timeoutMs = 2_500L,
                    ).execute(sourceCode)
                }

                // Animate each step cleanly on the board while staying responsive
                for ((index, step) in output.steps.withIndex()) {
                    _uiState.update {
                        it.copy(
                            board = step.boardSnapshot,
                            lastStepMessage = "${index + 1}. adım (Satır ${step.lineNumber}): ${step.message}",
                            activeMoveLine = step.lineNumber,
                            executionState = ExecutionUiState.Running(
                                "Adım ${index + 1}/${output.steps.size}: ${step.message}"
                            ),
                        )
                    }
                    delay(380L)
                }

                val err = output.error
                if (err != null) {
                    _uiState.update {
                        it.copy(
                            board = output.finalBoard,
                            executionState = ExecutionUiState.Error(
                                errorTitle = err.errorType,
                                turkishExplanation = err.turkishMessage,
                                lineNumber = err.lineNumber,
                                consoleOutput = output.consoleLines,
                            ),
                            activeMoveLine = err.lineNumber,
                            statusAnnouncement = "Hata: ${err.errorType}. ${err.turkishMessage}",
                        )
                    }
                } else if (output.finalBoard.isSolved()) {
                    repository.markLevelCompleted(level.id)
                    val updatedCompleted = _uiState.value.completedLevelIds + level.id
                    val successSummary = "Tebrikler! ${level.title} tamamlandı. \"kirmizi\" hedef blok ${output.steps.size} hareketle çıkışa ulaştı!"
                    _uiState.update {
                        it.copy(
                            board = output.finalBoard,
                            completedLevelIds = updatedCompleted,
                            executionState = ExecutionUiState.Solved(
                                summary = successSummary,
                                consoleOutput = output.consoleLines,
                                stepsCount = output.steps.size,
                            ),
                            activeMoveLine = null,
                            statusAnnouncement = successSummary,
                        )
                    }
                } else {
                    val remaining = output.finalBoard.distanceToExit()
                    val summary = buildString {
                        append("Kod hatasız çalıştı (${output.steps.size} hareket yapıldı), ancak \"kirmizi\" blok henüz çıkışa ulaşmadı.")
                        if (remaining != null && remaining > 0) {
                            append(" Çıkışa ulaşmak için sağa $remaining birim daha ilerlemesi gerekiyor.")
                        }
                    }
                    _uiState.update {
                        it.copy(
                            board = output.finalBoard,
                            executionState = ExecutionUiState.Incomplete(
                                summary = summary,
                                consoleOutput = output.consoleLines,
                                stepsCount = output.steps.size,
                            ),
                            activeMoveLine = null,
                            statusAnnouncement = summary,
                        )
                    }
                }
            } catch (_: CancellationException) {
                _uiState.update {
                    it.copy(
                        executionState = ExecutionUiState.Error(
                            errorTitle = "Çalıştırma Durduruldu",
                            turkishExplanation = "Kod çalıştırma işlemi kullanıcı tarafından durduruldu.",
                            lineNumber = null,
                            consoleOutput = emptyList(),
                        ),
                        statusAnnouncement = "Kod çalıştırma durduruldu.",
                    )
                }
            }
        }
    }

    fun stopExecution() {
        cancelExecutionInternal()
        _uiState.update {
            it.copy(
                executionState = ExecutionUiState.Error(
                    errorTitle = "Çalıştırma Durduruldu",
                    turkishExplanation = "Kodun çalışması kullanıcı tarafından durduruldu.",
                    lineNumber = null,
                    consoleOutput = emptyList(),
                ),
                statusAnnouncement = "Kod çalıştırma durduruldu.",
            )
        }
    }

    fun resetLevel() {
        cancelExecutionInternal()
        saveCodeJob?.cancel()
        val level = _uiState.value.currentLevel
        viewModelScope.launch {
            repository.clearCode(level.id)
        }
        _uiState.update {
            it.copy(
                code = level.starterCode,
                board = level.initialBoard,
                executionState = ExecutionUiState.Idle,
                revealedHintCount = 0,
                isHintVisible = false,
                lastStepMessage = null,
                activeMoveLine = null,
                statusAnnouncement = "${level.title} sıfırlandı. Tahta ve başlangıç kodu yenilendi.",
            )
        }
    }

    fun triggerHint() {
        _uiState.update { state ->
            val maxHints = state.currentLevel.hints.size
            val nextCount = if (!state.isHintVisible) {
                state.revealedHintCount.coerceAtLeast(1)
            } else if (state.revealedHintCount < maxHints) {
                state.revealedHintCount + 1
            } else {
                state.revealedHintCount
            }
            val activeHintText = state.currentLevel.hints.getOrNull(nextCount - 1)
            state.copy(
                isHintVisible = true,
                revealedHintCount = nextCount,
                statusAnnouncement = activeHintText ?: "İpucu açıldı.",
            )
        }
    }

    fun closeHint() {
        _uiState.update { it.copy(isHintVisible = false) }
    }

    fun setCommandGuideVisible(visible: Boolean) {
        _uiState.update { it.copy(isCommandGuideVisible = visible) }
    }

    fun goToNextLevel() {
        val nextId = _uiState.value.currentLevel.id + 1
        if (_uiState.value.levels.any { it.id == nextId }) {
            selectLevel(nextId)
        }
    }

    fun toggleLargeText() {
        val next = !_uiState.value.largeTextEnabled
        viewModelScope.launch {
            repository.setLargeText(next)
        }
        _uiState.update {
            it.copy(
                largeTextEnabled = next,
                statusAnnouncement = if (next) "Büyük yazı modu açıldı." else "Normal yazı boyutuna geçildi.",
            )
        }
    }

    fun toggleTheme(currentlyDark: Boolean) {
        _uiState.update {
            val nextDark = !currentlyDark
            it.copy(
                darkThemeOverride = nextDark,
                statusAnnouncement = if (nextDark) "Koyu tema açıldı." else "Açık tema açıldı.",
            )
        }
    }

    private fun cancelExecutionInternal() {
        executionJob?.cancel()
        executionJob = null
    }
}
