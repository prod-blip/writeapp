package com.atulpandey.clearwrite.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.zip.ZipInputStream

sealed interface ImportResult {
  data class Success(val text: String, val fileName: String) : ImportResult

  data class Failure(val message: String) : ImportResult
}

object DocumentImporter {
  private const val MAX_IMPORT_BYTES = 5L * 1024L * 1024L
  private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

  suspend fun import(context: Context, uri: Uri): ImportResult =
    withContext(Dispatchers.IO) {
      runCatching {
          val resolver = context.contentResolver
          val metadata = queryMetadata(context, uri)
          require(metadata.size == null || metadata.size <= MAX_IMPORT_BYTES) {
            "Choose a document smaller than 5 MB."
          }

          val isDocx = resolver.getType(uri) == DOCX_MIME || metadata.name.endsWith(".docx", ignoreCase = true)
          val text =
            resolver.openInputStream(uri)?.use { input ->
              if (isDocx) extractDocxText(ZipInputStream(input.buffered()))
              else input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("The selected document could not be opened.")

          require(text.isNotBlank()) { "The selected document does not contain readable text." }
          ImportResult.Success(text = text.trim(), fileName = metadata.name)
        }
        .getOrElse { error -> ImportResult.Failure(error.message ?: "The document could not be imported.") }
    }

  private fun queryMetadata(context: Context, uri: Uri): DocumentMetadata {
    var name = "Imported document"
    var size: Long? = null
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
          if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
      }
    return DocumentMetadata(name = name, size = size)
  }

  private fun extractDocxText(zip: ZipInputStream): String {
    while (true) {
      val entry = zip.nextEntry ?: break
      if (entry.name == "word/document.xml") {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply { setInput(zip, "UTF-8") }
        val output = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
          when (event) {
            XmlPullParser.START_TAG ->
              when (parser.name) {
                "t" -> output.append(parser.nextText())
                "tab" -> output.append('\t')
                "br" -> output.append('\n')
              }
            XmlPullParser.END_TAG -> if (parser.name == "p") output.append('\n')
          }
          event = parser.next()
        }
        return output.toString()
      }
      zip.closeEntry()
    }
    error("This DOCX file does not contain a readable document body.")
  }

  private data class DocumentMetadata(val name: String, val size: Long?)
}
