package com.example.ui.components.markdown

/**
 * Intermediate Abstract Syntax Tree (AST) representations of Markdown nodes.
 * Decouples Markdown parsing from Compose UI rendering.
 */
sealed class MarkdownNode {
    data class Heading(val level: Int, val text: String) : MarkdownNode()
    data class Blockquote(val text: String) : MarkdownNode()
    data class CodeBlock(val language: String?, val code: String) : MarkdownNode()
    data class BulletList(val items: List<String>) : MarkdownNode()
    data class NumberedList(val items: List<String>) : MarkdownNode()
    data class Checklist(val items: List<ChecklistItemNode>) : MarkdownNode()
    data class ImageNode(val url: String, val altText: String?) : MarkdownNode()
    data class CarouselNode(val images: List<ImageNode>) : MarkdownNode()
    data class VideoNode(val url: String, val title: String? = null) : MarkdownNode()
    data class Paragraph(val text: String) : MarkdownNode()
    object HorizontalRule : MarkdownNode()
}

data class ChecklistItemNode(val isChecked: Boolean, val text: String)
