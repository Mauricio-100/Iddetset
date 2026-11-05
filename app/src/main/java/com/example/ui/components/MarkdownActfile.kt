package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.widget.Toast

val LocalCommunityClickHandler = androidx.compose.runtime.compositionLocalOf<((String) -> Unit)?> { null }
val LocalChannelClickHandler = androidx.compose.runtime.compositionLocalOf<((String) -> Unit)?> { null }

// Markdown Block types
sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock()
    data class BulletList(val items: List<String>) : MarkdownBlock()
    data class NumberedList(val items: List<String>) : MarkdownBlock()
    data class Checklist(val items: List<ChecklistItem>) : MarkdownBlock()
    data class ImageBlock(val url: String, val altText: String?) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

data class ChecklistItem(val isChecked: Boolean, val text: String)

@Composable
fun MarkdownActfile(
    content: String,
    modifier: Modifier = Modifier,
    isMine: Boolean = false,
    compactOpenGraph: Boolean = false,
    truncateChars: Int? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null,
    onReadMoreClick: (() -> Unit)? = null
) {
    if (isVoiceMessage(content) || com.example.utils.AudioMessageHelper.isAudioContent(content)) {
        VoiceMessagePlayer(
            content = content,
            modifier = modifier,
            isMine = isMine
        )
        return
    }

    com.example.ui.components.markdown.MarkdownRenderer(
        content = content,
        modifier = modifier,
        isMine = isMine,
        compactOpenGraph = compactOpenGraph,
        truncateChars = truncateChars,
        onMentionClick = onMentionClick,
        onLinkClick = onLinkClick,
        onReadMoreClick = onReadMoreClick
    )
}

@Composable
fun rememberParsedMarkdown(content: String): List<MarkdownBlock> {
    return androidx.compose.runtime.remember(content) {
        parseMarkdownBlocks(content)
    }
}

@Composable
fun MarkdownText(
    annotatedString: AnnotatedString,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val communityClickHandler = LocalCommunityClickHandler.current
    val channelClickHandler = LocalChannelClickHandler.current
    
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = textColor),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val url = annotation.item
                    if (url.startsWith("https://iddet.app/c/") || url.startsWith("iddet://community/")) {
                        val slug = if (url.startsWith("https://iddet.app/c/")) url.substringAfter("https://iddet.app/c/").substringBefore("/") else url.substringAfter("iddet://community/").substringBefore("/")
                        if (slug.isNotBlank()) {
                            communityClickHandler?.invoke(slug)
                            return@ClickableText
                        }
                    }
                    if (onLinkClick != null) {
                        onLinkClick(url)
                    } else {
                        try {
                            uriHandler.openUri(url)
                        } catch (e: Exception) {
                            // ignore gracefully
                        }
                    }
                }
            
            annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick?.invoke(annotation.item)
                    return@ClickableText
                }
                
            annotatedString.getStringAnnotations(tag = "COMMUNITY", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    communityClickHandler?.invoke(annotation.item)
                    return@ClickableText
                }
                
            annotatedString.getStringAnnotations(tag = "CHANNEL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    channelClickHandler?.invoke(annotation.item)
                    return@ClickableText
                }
        }
    )
}

// Inline formatting parser
@Composable
fun parseRichMarkdownStyles(
    text: String,
    primaryColor: Color,
    isMine: Boolean = false
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val linkColor = if (isMine) Color.White else primaryColor
        val codeBgColor = if (isMine) Color.White.copy(alpha = 0.22f) else primaryColor.copy(alpha = 0.08f)
        val codeTextColor = if (isMine) Color.White else primaryColor
        
        while (i < text.length) {
            val isBoundary = (i == 0 || text[i - 1].isWhitespace() || "()[]{}<>,.?!:;\"'".contains(text[i - 1]))

            // 1. Mentions: @username, @c/community, @#channel
            if (text[i] == '@') {
                val mentionRegex = Regex("^(@c/([a-zA-Z0-9_-]+)|@#([a-zA-Z0-9_-]+)|@([a-zA-Z0-9_.]+))")
                val match = mentionRegex.find(text.substring(i))
                if (match != null) {
                    val fullMatch = match.value
                    
                    val mentionType = when {
                        fullMatch.startsWith("@c/") -> "COMMUNITY"
                        fullMatch.startsWith("@#") -> "CHANNEL"
                        else -> "MENTION" // user
                    }
                    val rawValue = when {
                        fullMatch.startsWith("@c/") -> fullMatch.substring(3)
                        fullMatch.startsWith("@#") -> fullMatch.substring(2)
                        else -> fullMatch.substring(1)
                    }
                    
                    val mentionColor = when(mentionType) {
                        "COMMUNITY" -> Color(0xFF8B5CF6) // Violet
                        "CHANNEL" -> Color(0xFF10B981) // Emerald
                        else -> linkColor
                    }

                    pushStringAnnotation(tag = mentionType, annotation = rawValue)
                    withStyle(SpanStyle(
                        color = mentionColor,
                        fontWeight = FontWeight.ExtraBold,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(fullMatch)
                    }
                    pop()
                    i += fullMatch.length
                    continue
                }
            }

            // 2. Community shortcut starting with / (e.g. /c/slug or /slug)
            if (text[i] == '/' && isBoundary) {
                val slashRegex = Regex("^(/c/([a-zA-Z0-9_-]+)|/([a-zA-Z0-9_-]{2,}))")
                val match = slashRegex.find(text.substring(i))
                if (match != null) {
                    val fullMatch = match.value
                    val slug = if (fullMatch.startsWith("/c/")) fullMatch.substring(3) else fullMatch.substring(1)
                    if (!fullMatch.startsWith("//") && slug.isNotEmpty() && !slug.equals("voice", ignoreCase = true)) {
                        pushStringAnnotation(tag = "COMMUNITY", annotation = slug)
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF8B5CF6),
                                fontWeight = FontWeight.ExtraBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(fullMatch)
                        }
                        pop()
                        i += fullMatch.length
                        continue
                    }
                }
            }

            // 3. Community shortcut starting with c/slug (e.g. c/general, c/crypto)
            if (text[i] == 'c' && i + 2 < text.length && text[i + 1] == '/' && isBoundary) {
                val cSlugRegex = Regex("^(c/([a-zA-Z0-9_-]+))")
                val match = cSlugRegex.find(text.substring(i))
                if (match != null) {
                    val fullMatch = match.value
                    val slug = fullMatch.substring(2)
                    pushStringAnnotation(tag = "COMMUNITY", annotation = slug)
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(fullMatch)
                    }
                    pop()
                    i += fullMatch.length
                    continue
                }
            }

            // 4. Channel shortcut starting with #channel (when not heading)
            if (text[i] == '#' && isBoundary && (i + 1 < text.length && !text[i + 1].isWhitespace())) {
                val channelRegex = Regex("^(#([a-zA-Z0-9_-]+))")
                val match = channelRegex.find(text.substring(i))
                if (match != null) {
                    val fullMatch = match.value
                    val channelName = fullMatch.substring(1)
                    pushStringAnnotation(tag = "CHANNEL", annotation = channelName)
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(fullMatch)
                    }
                    pop()
                    i += fullMatch.length
                    continue
                }
            }

            // Bold **
            if (i < text.length - 1 && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            // Bold __
            if (i < text.length - 1 && text[i] == '_' && text[i + 1] == '_') {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            // Italic *
            if (text[i] == '*') {
                val end = text.indexOf("*", i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // Italic _
            if (text[i] == '_') {
                val end = text.indexOf("_", i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // Strikethrough ~~
            if (i < text.length - 1 && text[i] == '~' && text[i + 1] == '~') {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            // Inline code `
            if (text[i] == '`') {
                val end = text.indexOf("`", i + 1)
                if (end != -1 && end > i + 1) {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor,
                        color = codeTextColor,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.9f
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // Link [text](url)
            if (text[i] == '[') {
                val closingBracket = text.indexOf(']', i + 1)
                if (closingBracket != -1) {
                    if (closingBracket < text.length - 1 && text[closingBracket + 1] == '(') {
                        val closingParen = text.indexOf(')', closingBracket + 2)
                        if (closingParen != -1) {
                            val linkText = text.substring(i + 1, closingBracket)
                            val url = text.substring(closingBracket + 2, closingParen)
                            
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            )) {
                                append(linkText)
                            }
                            pop()
                            i = closingParen + 1
                            continue
                        }
                    }
                }
            }
            
            // Raw HTTP URL check
            val rawUrlMatch = parseRawUrlAt(text, i)
            if (rawUrlMatch != null) {
                pushStringAnnotation(tag = "URL", annotation = rawUrlMatch)
                withStyle(SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )) {
                    append(rawUrlMatch)
                }
                pop()
                i += rawUrlMatch.length
                continue
            }
            
            append(text[i].toString())
            i++
        }
    }
}

// Helper to check if there's a raw URL at current index
fun parseRawUrlAt(text: String, index: Int): String? {
    if (index + 7 > text.length) return null
    val substring = text.substring(index)
    if (substring.startsWith("http://") || substring.startsWith("https://")) {
        val endOfUrl = substring.indexOfFirst { it.isWhitespace() || it == ')' || it == ']' || it == '}' || it == '*' || it == '~' }
        val len = if (endOfUrl == -1) substring.length else endOfUrl
        return substring.substring(0, len)
    }
    return null
}

// Block-by-block Markdown parser
fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = content.lines()
    var i = 0
    
    while (i < lines.size) {
        val line = lines[i]
        
        // 1. Code blocks
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim().ifEmpty { null }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            i++
            continue
        }
        
        // 2. Blockquotes
        if (line.trimStart().startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            quoteLines.add(line.trimStart().removePrefix(">").trim())
            i++
            while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                quoteLines.add(lines[i].trimStart().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
            continue
        }
        
        // 3. Headings
        if (line.trimStart().startsWith("#")) {
            val trimmed = line.trimStart()
            var level = 0
            while (level < trimmed.length && trimmed[level] == '#') {
                level++
            }
            if (level in 1..6 && level < trimmed.length && trimmed[level].isWhitespace()) {
                val headerText = trimmed.substring(level).trim()
                blocks.add(MarkdownBlock.Heading(level, headerText))
                i++
                continue
            }
        }
        
        // 4. Checklist items
        if (line.trimStart().startsWith("- [ ]") || line.trimStart().startsWith("- [x]") ||
            line.trimStart().startsWith("* [ ]") || line.trimStart().startsWith("* [x]")) {
            val checklistItems = mutableListOf<ChecklistItem>()
            var currentLine = line.trimStart()
            while (i < lines.size && (currentLine.startsWith("- [ ]") || currentLine.startsWith("- [x]") ||
                    currentLine.startsWith("* [ ]") || currentLine.startsWith("* [x]"))) {
                val isChecked = currentLine.contains("[x]") || currentLine.contains("[X]")
                val text = currentLine.substring(currentLine.indexOf(']') + 1).trim()
                checklistItems.add(ChecklistItem(isChecked, text))
                i++
                if (i < lines.size) currentLine = lines[i].trimStart()
            }
            blocks.add(MarkdownBlock.Checklist(checklistItems))
            continue
        }
        
        // 5. Bullet lists
        if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") || line.trimStart().startsWith("• ")) {
            val bulletItems = mutableListOf<String>()
            var currentLine = line.trimStart()
            while (i < lines.size && (currentLine.startsWith("- ") || currentLine.startsWith("* ") || currentLine.startsWith("• "))) {
                if (!currentLine.startsWith("- [") && !currentLine.startsWith("* [")) {
                    val text = currentLine.substring(2).trim()
                    bulletItems.add(text)
                }
                i++
                if (i < lines.size) currentLine = lines[i].trimStart()
            }
            blocks.add(MarkdownBlock.BulletList(bulletItems))
            continue
        }
        
        // 6. Numbered lists
        val numberRegex = Regex("^\\d+\\.\\s+(.*)")
        if (numberRegex.matches(line.trimStart())) {
            val numberedItems = mutableListOf<String>()
            var currentLine = line.trimStart()
            while (i < lines.size && numberRegex.matches(currentLine)) {
                val match = numberRegex.matchEntire(currentLine)
                val text = match?.groupValues?.get(1) ?: ""
                numberedItems.add(text)
                i++
                if (i < lines.size) currentLine = lines[i].trimStart()
            }
            blocks.add(MarkdownBlock.NumberedList(numberedItems))
            continue
        }
        
        // 7. Image Block ([IMG](url) or ![alt](url))
        val imgRegex = Regex("(?i)^\\[img\\]\\((.*?)\\)$")
        val altImgRegex = Regex("^!\\[(.*?)\\]\\((.*?)\\)$")
        val trimLine = line.trim()
        if (imgRegex.matches(trimLine)) {
            val url = imgRegex.matchEntire(trimLine)!!.groupValues[1]
            blocks.add(MarkdownBlock.ImageBlock(url, null))
            i++
            continue
        } else if (altImgRegex.matches(trimLine)) {
            val match = altImgRegex.matchEntire(trimLine)!!
            val alt = match.groupValues[1]
            val url = match.groupValues[2]
            blocks.add(MarkdownBlock.ImageBlock(url, alt))
            i++
            continue
        }
        
        // 8. Normal Paragraph (splits inline [IMG] or ![alt] visual attachments gracefully)
        if (line.trim().isNotEmpty()) {
            val mixed = parseMixedParagraph(line)
            blocks.addAll(mixed)
        }
        i++
    }
    
    return blocks
}

// Parses visual image splits inside paragraphs
fun parseMixedParagraph(text: String): List<MarkdownBlock> {
    val regex = Regex("(?i)(?:\\[img\\]|!\\[(.*?)\\])\\((.*?)\\)")
    val blocks = mutableListOf<MarkdownBlock>()
    var lastIndex = 0
    val matches = regex.findAll(text)
    
    for (match in matches) {
        val textBefore = text.substring(lastIndex, match.range.first)
        if (textBefore.trim().isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(textBefore))
        }
        val altText = match.groupValues[1].ifEmpty { null }
        val url = match.groupValues[2]
        blocks.add(MarkdownBlock.ImageBlock(url, altText))
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < text.length) {
        val remaining = text.substring(lastIndex)
        if (remaining.trim().isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(remaining))
        }
    }
    
    return blocks
}

// Helper to extract clean urls from markdown text for OpenGraph rendering
fun extractUrlsFromMarkdown(content: String): List<String> {
    val urls = mutableListOf<String>()
    
    // 1. Extract markdown links: [text](url)
    val markdownLinkRegex = Regex("\\[.*?\\]\\((https?://[^\n\\s)]+)\\)")
    markdownLinkRegex.findAll(content).forEach { match ->
        val url = match.groupValues[1]
        if (!urls.contains(url)) urls.add(url)
    }
    
    // 2. Extract raw URLs: https://... or http://...
    val rawUrlRegex = Regex("(?<!\\()(https?://[^\\s\\n\\])]+)")
    rawUrlRegex.findAll(content).forEach { match ->
        val url = match.groupValues[1]
        // Clean trailing punctuation
        var cleanUrl = url
        while (cleanUrl.endsWith(".") || cleanUrl.endsWith(",") || cleanUrl.endsWith("?") || cleanUrl.endsWith("!")) {
            cleanUrl = cleanUrl.dropLast(1)
        }
        if (!urls.contains(cleanUrl)) urls.add(cleanUrl)
    }
    
    return urls
}
