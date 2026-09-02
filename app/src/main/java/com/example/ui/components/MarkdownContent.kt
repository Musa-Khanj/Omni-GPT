package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary

sealed class ContentBlock {
    data class Paragraph(val text: String) : ContentBlock()
    data class Header(val level: Int, val text: String) : ContentBlock()
    data class BulletItem(val text: String) : ContentBlock()
    data class NumberedItem(val number: String, val text: String) : ContentBlock()
    data class Blockquote(val text: String) : ContentBlock()
    data class Code(val language: String, val code: String) : ContentBlock()
}

@Composable
fun MarkdownContent(
    content: String,
    onExecuteCode: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(content)

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 22.sp
                        2 -> 19.sp
                        else -> 17.sp
                    }
                    Text(
                        text = buildFormattedText(block.text),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                is ContentBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 10.dp, start = 4.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Text(
                            text = buildFormattedText(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is ContentBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}. ",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = buildFormattedText(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is ContentBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(EmeraldPrimary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildFormattedText(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                is ContentBlock.Paragraph -> {
                    Text(
                        text = buildFormattedText(block.text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                is ContentBlock.Code -> {
                    CodeBlockCard(
                        language = block.language,
                        code = block.code,
                        onExecute = onExecuteCode,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

fun parseMarkdownBlocks(text: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block check
        if (line.trim().startsWith("```")) {
            val lang = line.trim().removePrefix("```").trim()
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeBuilder.append(lines[i]).append("\n")
                i++
            }
            // remove trailing newline
            val code = codeBuilder.toString().trimEnd('\n')
            blocks.add(ContentBlock.Code(language = lang.ifEmpty { "code" }, code = code))
            i++
            continue
        }

        // Headers
        when {
            line.startsWith("### ") -> {
                blocks.add(ContentBlock.Header(3, line.removePrefix("### ").trim()))
            }
            line.startsWith("## ") -> {
                blocks.add(ContentBlock.Header(2, line.removePrefix("## ").trim()))
            }
            line.startsWith("# ") -> {
                blocks.add(ContentBlock.Header(1, line.removePrefix("# ").trim()))
            }
            line.trim().startsWith("> ") -> {
                blocks.add(ContentBlock.Blockquote(line.trim().removePrefix("> ").trim()))
            }
            line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                val bulletText = line.trim().substring(2).trim()
                blocks.add(ContentBlock.BulletItem(bulletText))
            }
            line.trim().matches(Regex("^\\d+\\.\\s.*")) -> {
                val match = Regex("^(\\d+)\\.\\s(.*)").find(line.trim())
                if (match != null) {
                    val num = match.groupValues[1]
                    val itemText = match.groupValues[2]
                    blocks.add(ContentBlock.NumberedItem(num, itemText))
                } else {
                    blocks.add(ContentBlock.Paragraph(line))
                }
            }
            line.isNotBlank() -> {
                // Group contiguous paragraphs
                val pBuilder = StringBuilder(line)
                while (i + 1 < lines.size &&
                    lines[i + 1].isNotBlank() &&
                    !lines[i + 1].trim().startsWith("```") &&
                    !lines[i + 1].startsWith("#") &&
                    !lines[i + 1].trim().startsWith("- ") &&
                    !lines[i + 1].trim().startsWith("* ") &&
                    !lines[i + 1].trim().startsWith("> ") &&
                    !lines[i + 1].trim().matches(Regex("^\\d+\\.\\s.*"))
                ) {
                    i++
                    pBuilder.append("\n").append(lines[i])
                }
                blocks.add(ContentBlock.Paragraph(pBuilder.toString()))
            }
        }
        i++
    }

    return blocks
}

@Composable
fun buildFormattedText(rawText: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val text = rawText

        // Pattern for inline code, bold, italics
        val regex = Regex("(`[^`]+`)|(\\*\\*[^\\*]+\\*\\*)|(\\*[\\*]*[^*]+\\*)|(__[^_]+__)")
        val matches = regex.findAll(text)

        matches.forEach { match ->
            val matchRange = match.range
            if (cursor < matchRange.first) {
                append(text.substring(cursor, matchRange.first))
            }

            val matchedValue = match.value
            when {
                matchedValue.startsWith("`") && matchedValue.endsWith("`") -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x33888888),
                            color = EmeraldPrimary,
                            fontSize = 13.5.sp
                        )
                    ) {
                        append(matchedValue.substring(1, matchedValue.length - 1))
                    }
                }
                matchedValue.startsWith("**") && matchedValue.endsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(matchedValue.substring(2, matchedValue.length - 2))
                    }
                }
                matchedValue.startsWith("*") && matchedValue.endsWith("*") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(matchedValue.substring(1, matchedValue.length - 1))
                    }
                }
                else -> {
                    append(matchedValue)
                }
            }
            cursor = matchRange.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
