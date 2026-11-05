package com.example.ui.components.markdown

/**
 * Robust, crash-resilient Markdown Parser layer.
 * Converts raw Markdown strings into an AST (List<MarkdownNode>).
 * Handles malformed input gracefully by falling back to Paragraph nodes.
 */
object MarkdownParser {

    fun parse(content: String?): List<MarkdownNode> {
        if (content.isNullOrBlank()) return emptyList()

        return try {
            parseInternal(content)
        } catch (e: Throwable) {
            // Absolute fallback: if any unexpected parsing error occurs, treat as simple paragraph
            listOf(MarkdownNode.Paragraph(content))
        }
    }

    private fun parseInternal(content: String): List<MarkdownNode> {
        val blocks = mutableListOf<MarkdownNode>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmedLine = line.trim()

            // 1. Code blocks (```)
            if (trimmedLine.startsWith("```")) {
                val lang = trimmedLine.removePrefix("```").trim().ifEmpty { null }
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownNode.CodeBlock(lang, codeLines.joinToString("\n")))
                i++
                continue
            }

            // 2. Horizontal Rules (---, ***, ___)
            if (isHorizontalRule(trimmedLine)) {
                blocks.add(MarkdownNode.HorizontalRule)
                i++
                continue
            }

            // 3. Blockquotes (>)
            if (trimmedLine.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                quoteLines.add(trimmedLine.removePrefix(">").trim())
                i++
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                blocks.add(MarkdownNode.Blockquote(quoteLines.joinToString("\n")))
                continue
            }

            // 4. Headings (#)
            if (trimmedLine.startsWith("#")) {
                var level = 0
                while (level < trimmedLine.length && trimmedLine[level] == '#') {
                    level++
                }
                if (level in 1..6 && level < trimmedLine.length && trimmedLine[level].isWhitespace()) {
                    val headerText = trimmedLine.substring(level).trim()
                    blocks.add(MarkdownNode.Heading(level, headerText))
                    i++
                    continue
                }
            }

            // 5. Checklist items (- [ ] / - [x] / * [ ])
            if (trimmedLine.startsWith("- [ ]") || trimmedLine.startsWith("- [x]") || trimmedLine.startsWith("- [X]") ||
                trimmedLine.startsWith("* [ ]") || trimmedLine.startsWith("* [x]") || trimmedLine.startsWith("* [X]")) {
                val checklistItems = mutableListOf<ChecklistItemNode>()
                var currentLine = trimmedLine
                while (i < lines.size && (currentLine.startsWith("- [") || currentLine.startsWith("* ["))) {
                    val isChecked = currentLine.contains("[x]") || currentLine.contains("[X]")
                    val closingBracketIdx = currentLine.indexOf(']')
                    val text = if (closingBracketIdx != -1 && closingBracketIdx < currentLine.length - 1) {
                        currentLine.substring(closingBracketIdx + 1).trim()
                    } else {
                        currentLine
                    }
                    checklistItems.add(ChecklistItemNode(isChecked, text))
                    i++
                    if (i < lines.size) currentLine = lines[i].trim()
                }
                blocks.add(MarkdownNode.Checklist(checklistItems))
                continue
            }

            // 6. Bullet lists (- , * , • )
            if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("• ")) {
                val bulletItems = mutableListOf<String>()
                var currentLine = trimmedLine
                while (i < lines.size && (currentLine.startsWith("- ") || currentLine.startsWith("* ") || currentLine.startsWith("• "))) {
                    if (!currentLine.startsWith("- [") && !currentLine.startsWith("* [")) {
                        val text = currentLine.substring(2).trim()
                        bulletItems.add(text)
                    }
                    i++
                    if (i < lines.size) currentLine = lines[i].trim()
                }
                if (bulletItems.isNotEmpty()) {
                    blocks.add(MarkdownNode.BulletList(bulletItems))
                }
                continue
            }

            // 7. Numbered lists (1. , 2. )
            val numberRegex = Regex("^\\d+\\.\\s+(.*)")
            if (numberRegex.matches(trimmedLine)) {
                val numberedItems = mutableListOf<String>()
                var currentLine = trimmedLine
                while (i < lines.size && numberRegex.matches(currentLine)) {
                    val match = numberRegex.matchEntire(currentLine)
                    val text = match?.groupValues?.get(1) ?: ""
                    numberedItems.add(text)
                    i++
                    if (i < lines.size) currentLine = lines[i].trim()
                }
                if (numberedItems.isNotEmpty()) {
                    blocks.add(MarkdownNode.NumberedList(numberedItems))
                }
                continue
            }

            // 8. Standalone Image / Video Node (![alt](url), [img](url), [Img](url), [video](url))
            val mediaRegex = Regex("(?i)^(?:!\\[(.*?)\\]|\\[(?:img|video|media|vid)\\])\\((.*?)\\)$")
            if (mediaRegex.matches(trimmedLine)) {
                val match = mediaRegex.matchEntire(trimmedLine)!!
                val alt = match.groupValues[1].ifEmpty { null }
                val url = match.groupValues[2].trim()
                if (com.example.ui.components.VideoUrlHelper.isVideoUrl(url)) {
                    blocks.add(MarkdownNode.VideoNode(url, alt))
                } else {
                    blocks.add(MarkdownNode.ImageNode(url, alt))
                }
                i++
                continue
            }

            // Standalone Direct Media URL on its own line
            if ((trimmedLine.startsWith("http://") || trimmedLine.startsWith("https://")) && !trimmedLine.contains(" ")) {
                if (com.example.ui.components.VideoUrlHelper.isVideoUrl(trimmedLine)) {
                    blocks.add(MarkdownNode.VideoNode(trimmedLine, null))
                    i++
                    continue
                } else if (isDirectImageUrl(trimmedLine)) {
                    blocks.add(MarkdownNode.ImageNode(trimmedLine, null))
                    i++
                    continue
                }
            }

            // 9. Standard Paragraph (with embedded visual image & video inline parsing)
            if (trimmedLine.isNotEmpty()) {
                val mixedNodes = parseMixedParagraph(line)
                blocks.addAll(mixedNodes)
            }
            i++
        }

        return postProcessCarousels(blocks)
    }

    private fun isDirectImageUrl(url: String): Boolean {
        val clean = url.trim().lowercase()
        return clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") ||
                clean.endsWith(".webp") || clean.endsWith(".gif") || clean.endsWith(".bmp") ||
                (clean.contains("cloudinary.com") && (clean.contains("/image/upload") || clean.contains("/upload/")))
    }

    private fun postProcessCarousels(nodes: List<MarkdownNode>): List<MarkdownNode> {
        val result = mutableListOf<MarkdownNode>()
        val pendingImages = mutableListOf<MarkdownNode.ImageNode>()

        val flushImages = {
            if (pendingImages.size >= 2) {
                result.add(MarkdownNode.CarouselNode(pendingImages.toList()))
            } else if (pendingImages.size == 1) {
                result.add(pendingImages.first())
            }
            pendingImages.clear()
        }

        for (node in nodes) {
            if (node is MarkdownNode.ImageNode) {
                if (com.example.ui.components.VideoUrlHelper.isVideoUrl(node.url)) {
                    flushImages()
                    result.add(MarkdownNode.VideoNode(node.url, node.altText))
                } else {
                    pendingImages.add(node)
                }
            } else {
                flushImages()
                result.add(node)
            }
        }
        flushImages()
        return result
    }

    private fun isHorizontalRule(line: String): Boolean {
        val clean = line.replace(" ", "")
        return clean == "---" || clean == "***" || clean == "___" ||
                (clean.length >= 3 && clean.all { it == '-' } || clean.all { it == '*' } || clean.all { it == '_' })
    }

    private fun parseMixedParagraph(text: String): List<MarkdownNode> {
        val regex = Regex("(?i)(?:!\\[(.*?)\\]|\\[(?:img|video|media|vid)\\])\\((.*?)\\)")
        val nodes = mutableListOf<MarkdownNode>()
        var lastIndex = 0
        val matches = regex.findAll(text)

        for (match in matches) {
            val textBefore = text.substring(lastIndex, match.range.first)
            if (textBefore.trim().isNotEmpty()) {
                nodes.add(MarkdownNode.Paragraph(textBefore))
            }
            val altText = match.groupValues[1].ifEmpty { null }
            val url = match.groupValues[2].trim()
            if (com.example.ui.components.VideoUrlHelper.isVideoUrl(url)) {
                nodes.add(MarkdownNode.VideoNode(url, altText))
            } else {
                nodes.add(MarkdownNode.ImageNode(url, altText))
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            val remaining = text.substring(lastIndex)
            if (remaining.trim().isNotEmpty()) {
                nodes.add(MarkdownNode.Paragraph(remaining))
            }
        }

        return nodes
    }
}
