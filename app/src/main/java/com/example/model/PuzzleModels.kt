package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BlockBlue
import com.example.ui.theme.BlockBlueBorder
import com.example.ui.theme.BlockGreen
import com.example.ui.theme.BlockGreenBorder
import com.example.ui.theme.BlockOrange
import com.example.ui.theme.BlockOrangeBorder
import com.example.ui.theme.BlockPurple
import com.example.ui.theme.BlockPurpleBorder
import com.example.ui.theme.BlockRed
import com.example.ui.theme.BlockRedBorder
import com.example.ui.theme.BlockYellow
import com.example.ui.theme.BlockYellowBorder

enum class Orientation(val turkishLabel: String, val axisHint: String) {
    HORIZONTAL("Yatay", "sağa veya sola"),
    VERTICAL("Dikey", "yukarı veya aşağı"),
}

enum class Direction(
    val codeName: String,
    val turkishDisplay: String,
    val dRow: Int,
    val dCol: Int,
) {
    SAG("sag", "sağa", 0, 1),
    SOL("sol", "sola", 0, -1),
    YUKARI("yukari", "yukarı", -1, 0),
    ASAGI("asagi", "aşağı", 1, 0);

    fun matchesOrientation(orientation: Orientation): Boolean = when (orientation) {
        Orientation.HORIZONTAL -> this == SAG || this == SOL
        Orientation.VERTICAL -> this == YUKARI || this == ASAGI
    }

    companion object {
        fun parse(raw: String): Direction? = when (normalizeTurkishToken(raw)) {
            "sag", "saga" -> SAG
            "sol", "sola" -> SOL
            "yukari", "yukariya" -> YUKARI
            "asagi", "asagiya" -> ASAGI
            else -> null
        }
    }
}

enum class BlockPalette(
    val fill: Color,
    val border: Color,
    val contentOnColor: Color,
    val colorNameTr: String,
) {
    RED(BlockRed, BlockRedBorder, Color.White, "Kırmızı"),
    BLUE(BlockBlue, BlockBlueBorder, Color.White, "Mavi"),
    GREEN(BlockGreen, BlockGreenBorder, Color.White, "Yeşil"),
    ORANGE(BlockOrange, BlockOrangeBorder, Color.White, "Turuncu"),
    PURPLE(BlockPurple, BlockPurpleBorder, Color.White, "Mor"),
    YELLOW(BlockYellow, BlockYellowBorder, Color(0xFF111827), "Sarı"),
}

data class PuzzleBlock(
    val id: String,
    val displayName: String,
    val palette: BlockPalette,
    val orientation: Orientation,
    val row: Int,
    val col: Int,
    val length: Int,
    val isTarget: Boolean = false,
) {
    fun occupiedCells(): List<Pair<Int, Int>> = (0 until length).map { offset ->
        when (orientation) {
            Orientation.HORIZONTAL -> row to (col + offset)
            Orientation.VERTICAL -> (row + offset) to col
        }
    }

    fun positionDescription(): String = when (orientation) {
        Orientation.HORIZONTAL -> {
            val endCol = col + length - 1
            "Satır $row, Sütun $col–$endCol"
        }
        Orientation.VERTICAL -> {
            val endRow = row + length - 1
            "Satır $row–$endRow, Sütun $col"
        }
    }
}

sealed class MoveOutcome {
    data class Success(
        val newBoard: BoardState,
        val description: String,
    ) : MoveOutcome()

    data class Failure(
        val turkishReason: String,
    ) : MoveOutcome()
}

data class BoardState(
    val gridSize: Int = 6,
    val exitRow: Int = 3,
    val blocks: List<PuzzleBlock>,
) {
    fun findBlock(rawId: String): PuzzleBlock? {
        val normalized = normalizeTurkishToken(rawId)
        return blocks.firstOrNull { normalizeTurkishToken(it.id) == normalized }
    }

    fun isSolved(): Boolean {
        val target = blocks.firstOrNull { it.isTarget } ?: return false
        return target.orientation == Orientation.HORIZONTAL &&
            target.row == exitRow &&
            (target.col + target.length - 1) == gridSize
    }

    fun distanceToExit(): Int? {
        val target = blocks.firstOrNull { it.isTarget } ?: return null
        if (target.orientation != Orientation.HORIZONTAL) return null
        return (gridSize - (target.col + target.length - 1)).coerceAtLeast(0)
    }

    fun availableSteps(blockId: String, direction: Direction): Int {
        val block = findBlock(blockId) ?: return 0
        if (!direction.matchesOrientation(block.orientation)) return 0
        var steps = 0
        var current = block
        while (true) {
            val next = current.copy(
                row = current.row + direction.dRow,
                col = current.col + direction.dCol,
            )
            if (isOutOfBounds(next) || collidesWithOther(next, excludeId = block.id) != null) {
                break
            }
            steps++
            current = next
        }
        return steps
    }

    fun canMoveOneStep(blockId: String, direction: Direction): Boolean =
        availableSteps(blockId, direction) >= 1

    fun moveBlock(rawBlockId: String, rawDirection: String, steps: Int): MoveOutcome {
        val block = findBlock(rawBlockId)
            ?: return MoveOutcome.Failure(
                "\"$rawBlockId\" adında bir blok tahtada bulunamadı. Geçerli blok adları: ${
                    blocks.joinToString(", ") { "\"${it.id}\"" }
                }."
            )

        val direction = Direction.parse(rawDirection)
            ?: return MoveOutcome.Failure(
                "\"$rawDirection\" geçerli bir yön değil. Kullanabileceğin yönler: \"sag\", \"sol\", \"yukari\", \"asagi\"."
            )

        if (steps <= 0) {
            return MoveOutcome.Failure(
                "Adım sayısı 0'dan büyük bir tam sayı olmalıdır (girilen: $steps)."
            )
        }

        if (!direction.matchesOrientation(block.orientation)) {
            return MoveOutcome.Failure(
                "\"${block.id}\" (${block.displayName}) ${block.orientation.turkishLabel.lowercase()} bir bloktur; " +
                    "yalnızca ${block.orientation.axisHint} hareket edebilir."
            )
        }

        var workingBlock = block
        for (stepIndex in 1..steps) {
            val nextBlock = workingBlock.copy(
                row = workingBlock.row + direction.dRow,
                col = workingBlock.col + direction.dCol,
            )
            if (isOutOfBounds(nextBlock)) {
                return MoveOutcome.Failure(
                    "Duvara çarpma: \"${block.id}\" bloğu ${direction.turkishDisplay} yönde " +
                        "$stepIndex. adımda tahta sınırına ulaştı (6x6 tahta sınırı)."
                )
            }
            val blocker = collidesWithOther(nextBlock, excludeId = block.id)
            if (blocker != null) {
                return MoveOutcome.Failure(
                    "Engel var: \"${block.id}\" bloğu ${direction.turkishDisplay} yönde " +
                        "$stepIndex. adımda \"${blocker.id}\" (${blocker.displayName}) bloğuna takıldı " +
                        "(${blocker.positionDescription()})."
                )
            }
            workingBlock = nextBlock
        }

        val updatedBlocks = blocks.map { if (it.id == block.id) workingBlock else it }
        val updatedBoard = copy(blocks = updatedBlocks)
        val desc = "\"${block.id}\" bloğu $steps adım ${direction.turkishDisplay} kaydırıldı -> ${workingBlock.positionDescription()}."
        return MoveOutcome.Success(updatedBoard, desc)
    }

    private fun isOutOfBounds(block: PuzzleBlock): Boolean =
        block.occupiedCells().any { (r, c) -> r < 1 || r > gridSize || c < 1 || c > gridSize }

    private fun collidesWithOther(candidate: PuzzleBlock, excludeId: String): PuzzleBlock? {
        val candidateCells = candidate.occupiedCells().toSet()
        return blocks.firstOrNull { other ->
            other.id != excludeId && other.occupiedCells().any { it in candidateCells }
        }
    }

    fun toTurkishTextSummary(): String {
        val sb = StringBuilder()
        sb.append("Tahta ${gridSize}x${gridSize} boyutunda. Çıkış kapısı Satır $exitRow, Sütun $gridSize sağ kenarındadır.\n")
        blocks.forEach { block ->
            val role = if (block.isTarget) " [HEDEF BLOK]" else ""
            sb.append("• ${block.id} (${block.displayName}$role, ${block.orientation.turkishLabel}, ${block.length} birim): ${block.positionDescription()}\n")
        }
        val dist = distanceToExit()
        if (dist != null) {
            if (isSolved()) {
                sb.append("Durum: Kırmızı hedef blok çıkışa ulaştı!")
            } else {
                sb.append("Durum: \"kirmizi\" bloğun çıkışa ulaşması için sağa $dist birim daha ilerlemesi gerekiyor.")
            }
        }
        return sb.toString().trim()
    }
}

data class CommandDoc(
    val signature: String,
    val description: String,
    val example: String,
)

data class PuzzleLevel(
    val id: Int,
    val title: String,
    val conceptTitle: String,
    val conceptExplanation: String,
    val taskDescription: String,
    val starterCode: String,
    val hints: List<String>,
    val initialBoard: BoardState,
    val commandDocs: List<CommandDoc>,
)

data class ExecutionStep(
    val boardSnapshot: BoardState,
    val message: String,
    val lineNumber: Int,
)

sealed class ExecutionUiState {
    data object Idle : ExecutionUiState()
    data class Running(val statusText: String) : ExecutionUiState()
    data class Solved(
        val summary: String,
        val consoleOutput: List<String>,
        val stepsCount: Int,
    ) : ExecutionUiState()
    data class Incomplete(
        val summary: String,
        val consoleOutput: List<String>,
        val stepsCount: Int,
    ) : ExecutionUiState()
    data class Error(
        val errorTitle: String,
        val turkishExplanation: String,
        val lineNumber: Int?,
        val consoleOutput: List<String>,
    ) : ExecutionUiState()
}

fun normalizeTurkishToken(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input.trim()) {
        val mapped = when (ch) {
            'İ', 'I', 'ı' -> 'i'
            'Ğ', 'ğ' -> 'g'
            'Ü', 'ü' -> 'u'
            'Ş', 'ş' -> 's'
            'Ö', 'ö' -> 'o'
            'Ç', 'ç' -> 'c'
            else -> ch.lowercaseChar()
        }
        sb.append(mapped)
    }
    return sb.toString()
}
