package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoardState
import com.example.model.Orientation
import com.example.model.PuzzleBlock
import com.example.ui.theme.AmberExitGold
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun GameBoardSection(
    board: BoardState,
    lastStepMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Oyun Tahtası (6×6)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    val remaining = board.distanceToExit()
                    val statusChipText = if (board.isSolved()) {
                        "Çıkışa ulaştı!"
                    } else {
                        "Çıkışa $remaining birim"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (board.isSolved()) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = statusChipText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (board.isSolved()) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_board_grid"),
                    contentAlignment = Alignment.Center,
                ) {
                    val labelGutter = 24.dp
                    val exitGateWidth = 54.dp
                    val usableWidth = (maxWidth - labelGutter - exitGateWidth - 12.dp).coerceIn(180.dp, 360.dp)
                    val cellSize = usableWidth / board.gridSize
                    val boardSide = cellSize * board.gridSize

                    Column(horizontalAlignment = Alignment.Start) {
                        // Top column numbers 1..6
                        Row(
                            modifier = Modifier.padding(start = labelGutter),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (col in 1..board.gridSize) {
                                Text(
                                    text = col.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(cellSize),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            // Left row numbers 1..6
                            Column(modifier = Modifier.width(labelGutter)) {
                                for (row in 1..board.gridSize) {
                                    Box(
                                        modifier = Modifier.size(width = labelGutter, height = cellSize),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = row.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            // Grid + blocks
                            Box(
                                modifier = Modifier
                                    .size(boardSide)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                            ) {
                                // Empty grid cells
                                Column(modifier = Modifier.fillMaxSize()) {
                                    for (r in 1..board.gridSize) {
                                        Row {
                                            for (c in 1..board.gridSize) {
                                                val isExitLane = r == board.exitRow
                                                Box(
                                                    modifier = Modifier
                                                        .size(cellSize)
                                                        .background(
                                                            if (isExitLane) {
                                                                AmberExitGold.copy(alpha = 0.10f)
                                                            } else {
                                                                Color.Transparent
                                                            }
                                                        )
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Animated puzzle blocks
                                board.blocks.forEach { block ->
                                    AnimatedBoardBlock(
                                        block = block,
                                        cellSize = cellSize,
                                    )
                                }
                            }

                            // Golden Exit Gate aligned to exitRow
                            Box(
                                modifier = Modifier
                                    .width(exitGateWidth)
                                    .height(boardSide),
                            ) {
                                val exitOffsetY = cellSize * (board.exitRow - 1)
                                Row(
                                    modifier = Modifier
                                        .offset(x = 4.dp, y = exitOffsetY)
                                        .height(cellSize)
                                        .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                        .background(AmberExitGold.copy(alpha = 0.22f))
                                        .border(
                                            width = 2.dp,
                                            color = AmberExitGold,
                                            shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                                        )
                                        .padding(horizontal = 4.dp)
                                        .semantics {
                                            contentDescription = "Çıkış kapısı, Satır ${board.exitRow} sağ kenarı"
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = AmberExitGold,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "ÇIKIŞ",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                if (lastStepMessage != null) {
                    Text(
                        text = "Son adım: $lastStepMessage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Accessible text representation of board state
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .testTag("board_text_summary"),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Tahtanın Metin Durumu",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = board.toTurkishTextSummary(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AnimatedBoardBlock(
    block: PuzzleBlock,
    cellSize: Dp,
) {
    val padding = 3.dp
    val targetX = cellSize * (block.col - 1) + padding
    val targetY = cellSize * (block.row - 1) + padding

    val animX by animateDpAsState(
        targetValue = targetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "blockX_${block.id}",
    )
    val animY by animateDpAsState(
        targetValue = targetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "blockY_${block.id}",
    )

    val width = when (block.orientation) {
        Orientation.HORIZONTAL -> cellSize * block.length - padding * 2
        Orientation.VERTICAL -> cellSize - padding * 2
    }
    val height = when (block.orientation) {
        Orientation.HORIZONTAL -> cellSize - padding * 2
        Orientation.VERTICAL -> cellSize * block.length - padding * 2
    }

    val semanticText = "${block.displayName}, kod adı ${block.id}, ${block.orientation.turkishLabel}, ${block.positionDescription()}" +
        if (block.isTarget) ", hedef blok" else ""

    Box(
        modifier = Modifier
            .offset(x = animX, y = animY)
            .size(width = width, height = height)
            .shadow(3.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(block.palette.fill)
            .border(
                width = if (block.isTarget) 3.dp else 2.dp,
                color = if (block.isTarget) Color(0xFFFDE047) else block.palette.border,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("block_${block.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = semanticText
            },
        contentAlignment = Alignment.Center,
    ) {
        if (block.orientation == Orientation.HORIZONTAL) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (block.isTarget) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        tint = block.palette.contentOnColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = block.id,
                        color = block.palette.contentOnColor,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (block.isTarget) "Hedef" else block.palette.colorNameTr,
                        color = block.palette.contentOnColor.copy(alpha = 0.92f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = null,
                    tint = block.palette.contentOnColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = block.id,
                    color = block.palette.contentOnColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = block.palette.colorNameTr,
                    color = block.palette.contentOnColor.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}
