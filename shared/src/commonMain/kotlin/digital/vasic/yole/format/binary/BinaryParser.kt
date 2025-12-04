/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Binary/Embed Parser for Kotlin Multiplatform
 * Supports handling binary files and embedded content
 *
 *########################################################*/
package digital.vasic.yole.format.binary

import digital.vasic.yole.format.*

/**
 * Parser for binary files and embedded content
 * Handles images, PDFs, audio, video, and other binary formats
 */
class BinaryParser : TextParser {
    
    override val supportedFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_BINARY }
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        val fileSize = options["fileSize"] as? Long ?: 0L
        val mimeType = options["mimeType"] as? String ?: detectMimeType(filename)
        
        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = generateContentPreview(mimeType, filename, content),
            metadata = buildMap {
                put("mime_type", mimeType)
                put("file_size", fileSize.toString())
                put("is_binary", "true")
                put("file_type", getFileType(mimeType))
                put("extension", filename.substringAfterLast('.', ""))
            }
        )
    }
    
    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        val themeClass = if (lightMode) "light" else "dark"
        val mimeType = document.metadata["mime_type"] ?: ""
        val filename = document.metadata["filename"] ?: ""
        val fileSize = document.metadata["file_size"]?.toLongOrNull() ?: 0L
        
        return """
            |<div class="binary-file $themeClass">
            |<div class="binary-header">
            |  <h1>Binary File Preview</h1>
            |  <div class="file-info">
            |    <span class="filename">${escapeHtml(filename)}</span>
            |    <span class="file-type">${getFileType(mimeType)}</span>
            |    <span class="file-size">${formatFileSize(fileSize)}</span>
            |  </div>
            |</div>
            |<div class="binary-content">
            |${generateContentPreview(mimeType, filename, document.rawContent)}
            |</div>
            |</div>
            |<style>
            |.binary-file { font-family: sans-serif; line-height: 1.6; }
            |.binary-file.light { background: white; color: black; }
            |.binary-file.dark { background: #1e1e1e; color: #d4d4d4; }
            |.binary-header { border-bottom: 2px solid #75507b; padding: 1rem; margin-bottom: 1rem; }
            |.file-info { display: flex; gap: 1rem; font-size: 0.9rem; color: #666; }
            |.file-info .dark { color: #aaa; }
            |.binary-content { padding: 1rem; text-align: center; }
            |.image-preview { max-width: 100%; max-height: 400px; border-radius: 4px; }
            |.audio-preview { width: 100%; max-width: 400px; }
            |.video-preview { width: 100%; max-width: 600px; }
            |.pdf-preview { width: 100%; height: 600px; border: 1px solid #ddd; }
            |.pdf-preview .dark { border-color: #444; }
            |.generic-preview { background: #f5f5f5; border: 1px solid #ddd; border-radius: 4px; padding: 2rem; margin: 1rem 0; }
            |.generic-preview .dark { background: #2d2d2d; border-color: #444; }
            |.file-icon { font-size: 4rem; margin-bottom: 1rem; }
            |.download-button { 
            |  background: #4e9a06; color: white; border: none; padding: 0.5rem 1rem; 
            |  border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block;
            |}
            |.download-button:hover { background: #73d216; }
            |</style>
        """.trimMargin()
    }
    
    override fun canParse(format: TextFormat): Boolean {
        return supportedFormat.id == format.id
    }
    
    override fun validate(content: String): List<String> {
        // Binary content is always valid
        return emptyList()
    }
    
    private fun detectMimeType(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "zip" -> "application/zip"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            else -> "application/octet-stream"
        }
    }
    
    private fun getFileType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "Image"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType.startsWith("video/") -> "Video"
            mimeType == "application/pdf" -> "PDF Document"
            mimeType.startsWith("application/") -> "Document"
            else -> "Binary File"
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    private fun generateContentPreview(mimeType: String, filename: String, content: String): String {
        return when {
            mimeType.startsWith("image/") -> generateImagePreview(filename, content)
            mimeType.startsWith("audio/") -> generateAudioPreview(filename, content)
            mimeType.startsWith("video/") -> generateVideoPreview(filename, content)
            mimeType == "application/pdf" -> generatePdfPreview(filename, content)
            else -> generateGenericPreview(mimeType, filename)
        }
    }
    
    private fun generateImagePreview(filename: String, content: String): String {
        return """
            |<div class="image-container">
            |  <div class="file-icon">🖼️</div>
            |  <p>Image file: ${escapeHtml(filename)}</p>
            |  <p><em>Image preview would be displayed here</em></p>
            |  <a href="#" class="download-button">Download Image</a>
            |</div>
        """.trimMargin()
    }
    
    private fun generateAudioPreview(filename: String, content: String): String {
        return """
            |<div class="audio-container">
            |  <div class="file-icon">🎵</div>
            |  <p>Audio file: ${escapeHtml(filename)}</p>
            |  <audio controls class="audio-preview">
            |    <source src="#" type="audio/mpeg">
            |    Your browser does not support the audio element.
            |  </audio>
            |  <br>
            |  <a href="#" class="download-button">Download Audio</a>
            |</div>
        """.trimMargin()
    }
    
    private fun generateVideoPreview(filename: String, content: String): String {
        return """
            |<div class="video-container">
            |  <div class="file-icon">🎬</div>
            |  <p>Video file: ${escapeHtml(filename)}</p>
            |  <video controls class="video-preview">
            |    <source src="#" type="video/mp4">
            |    Your browser does not support the video element.
            |  </video>
            |  <br>
            |  <a href="#" class="download-button">Download Video</a>
            |</div>
        """.trimMargin()
    }
    
    private fun generatePdfPreview(filename: String, content: String): String {
        return """
            |<div class="pdf-container">
            |  <div class="file-icon">📄</div>
            |  <p>PDF Document: ${escapeHtml(filename)}</p>
            |  <iframe src="#" class="pdf-preview" title="PDF Preview">
            |    Your browser does not support PDF preview.
            |  </iframe>
            |  <br>
            |  <a href="#" class="download-button">Download PDF</a>
            |</div>
        """.trimMargin()
    }
    
    private fun generateGenericPreview(mimeType: String, filename: String): String {
        val icon = when (mimeType) {
            "application/zip", "application/x-tar", "application/gzip" -> "📦"
            else -> "📁"
        }
        
        return """
            |<div class="generic-preview">
            |  <div class="file-icon">$icon</div>
            |  <h3>Binary File</h3>
            |  <p>File: ${escapeHtml(filename)}</p>
            |  <p>Type: ${getFileType(mimeType)}</p>
            |  <p>MIME Type: $mimeType</p>
            |  <p><em>This file type cannot be previewed in the editor.</em></p>
            |  <a href="#" class="download-button">Download File</a>
            |</div>
        """.trimMargin()
    }
    
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

/**
 * Register the Binary parser with the registry
 */
fun registerBinaryParser() {
    ParserRegistry.register(BinaryParser())
}