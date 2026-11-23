package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorBackground
import com.example.ui.theme.EditorBorder
import com.example.ui.theme.EditorGutterBackground
import com.example.ui.theme.EditorGutterText
import com.example.ui.theme.EditorText
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodeEditorPanel(
    code: String,
    activeLine: Int?,
    largeText: Boolean,
    darkThemeOverride: Boolean?,
    editorFocusRequester: FocusRequester,
    runButtonFocusRequester: FocusRequester,
    onCodeChange: (String) -> Unit,
    onRunRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep selection state synced when level changes or reset occurs
    var selection by remember { mutableStateOf(TextRange(code.length)) }
    val clampedSelection = TextRange(
        start = selection.start.coerceIn(0, code.length),
        end = selection.end.coerceIn(0, code.length),
    )
    val textFieldValue = TextFieldValue(text = code, selection = clampedSelection)

    val lineCount = remember(code) { code.split('\n').size.coerceAtLeast(1) }
    val codeFontSize = if (largeText) 17.sp else 15.sp
    val codeLineHeight = if (largeText) 25.sp else 22.sp

    fun insertAtCursor(snippet: String) {
        val start = clampedSelection.min
        val end = clampedSelection.max
        val updated = code.substring(0, start) + snippet + code.substring(end)
        val newCursor = start + snippet.length
        selection = TextRange(newCursor)
        onCodeChange(updated)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
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
                    text = "Python Kod Alanı",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Ctrl+Enter: Çalıştır • Esc: Odaktan çık",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Quick-insert helper chips for keyboard & mobile ease
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val snippets = listOf(
                    "4 Boşluk" to "    ",
                    "hareket_et(...)" to "hareket_et(\"kirmizi\", \"sag\", 1)",
                    "yol_acik_mi(...)" to "yol_acik_mi(\"kirmizi\", \"sag\")",
                    "for döngüsü" to "for adim in range(2):\n    ",
                    "if koşulu" to "if yol_acik_mi(\"kirmizi\", \"sag\"):\n    ",
                )
                snippets.forEach { (label, snippet) ->
                    OutlinedButton(
                        onClick = { insertAtCursor(snippet) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .accessibleFocusRing(RoundedCornerShape(10.dp), darkThemeOverride = darkThemeOverride)
                            .semantics {
                                contentDescription = "$label kod parçasını ekle"
                            },
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                        )
                    }
                }
            }

            // Monospace editor box with line-number gutter
            val horizontalScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EditorBackground)
                    .border(1.5.dp, EditorBorder, RoundedCornerShape(12.dp))
                    .accessibleFocusRing(RoundedCornerShape(12.dp), darkThemeOverride = true),
            ) {
                // Gutter
                Column(
                    modifier = Modifier
                        .width(42.dp)
                        .background(EditorGutterBackground)
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    for (lineNum in 1..lineCount) {
                        val isHighlighted = activeLine == lineNum
                        Text(
                            text = lineNum.toString(),
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = codeFontSize,
                                lineHeight = codeLineHeight,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHighlighted) Color(0xFFFACC15) else EditorGutterText,
                                textAlign = TextAlign.End,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Editable code field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                        .padding(12.dp),
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { updated ->
                            selection = updated.selection
                            if (updated.text != code) {
                                onCodeChange(updated.text)
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = codeFontSize,
                            lineHeight = codeLineHeight,
                            color = EditorText,
                        ),
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 186.dp)
                            .focusRequester(editorFocusRequester)
                            .testTag("python_code_editor")
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when {
                                        event.key == Key.Enter && event.isCtrlPressed -> {
                                            onRunRequested()
                                            true
                                        }
                                        event.key == Key.F5 -> {
                                            onRunRequested()
                                            true
                                        }
                                        event.key == Key.Escape -> {
                                            runButtonFocusRequester.requestFocus()
                                            true
                                        }
                                        event.key == Key.Tab && !event.isShiftPressed -> {
                                            insertAtCursor("    ")
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .semantics {
                                contentDescription = "Python kod düzenleyici, $lineCount satır. Kodu çalıştırmak için Ctrl+Enter, alandan çıkmak için Escape tuşuna basın."
                            },
                    )
                }
            }
        }
    }
}
