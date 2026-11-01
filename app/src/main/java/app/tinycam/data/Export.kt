package app.tinycam.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies finished recordings into a folder the parents picked with the system folder picker
 * (Storage Access Framework). Snaplet only writes into that folder; whatever happens to it later
 * (gallery, sync apps) is up to the apps the parents use.
 */
class MediaExporter(private val context: Context) {
    private val resolver get() = context.contentResolver

    /** Keeps write access to [tree] across restarts and drops access to any previously chosen folder. */
    fun adopt(tree: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        resolver.takePersistableUriPermission(tree, flags)
        resolver.persistedUriPermissions.filter { it.uri != tree }.forEach {
            runCatching { resolver.releasePersistableUriPermission(it.uri, flags) }
        }
    }

    fun hasAccess(tree: Uri) = resolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission }

    /** Copies [item] into [tree]; returns false if the folder is gone or not writable. */
    suspend fun export(item: MediaItem, tree: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val mime = if (item.isVideo) "video/mp4" else "image/jpeg"
            // Providers append " (1)" etc. if the name exists, so an earlier copy is never overwritten
            val target = DocumentsContract.createDocument(resolver, parent, mime, item.file.name) ?: return@runCatching false
            val copied = runCatching {
                resolver.openOutputStream(target)?.use { out -> item.file.inputStream().use { it.copyTo(out) } } != null
            }.getOrDefault(false)
            // No half-written files in the parents' folder
            if (!copied) runCatching { DocumentsContract.deleteDocument(resolver, target) }
            copied
        }.getOrDefault(false)
    }

    companion object {
        /** Readable folder name, e.g. "DCIM/Snaplet" for "primary:DCIM/Snaplet". */
        fun label(tree: Uri): String =
            runCatching { DocumentsContract.getTreeDocumentId(tree).substringAfter(':').ifEmpty { "/" } }
                .getOrDefault(tree.lastPathSegment.orEmpty())
    }
}
