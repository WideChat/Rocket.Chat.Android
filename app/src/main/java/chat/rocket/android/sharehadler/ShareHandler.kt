package chat.rocket.android.sharehadler

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import java.io.InputStream

object ShareHandler {

    fun hasShare(): Boolean = hasSharedText() || hasSharedFile()

    fun hasSharedText(): Boolean = sharedText != null
    fun hasSharedFile(): Boolean = files.size > 0

    var sharedText: String? = null

    var files: ArrayList<SharedFile> = arrayListOf()

    fun handle(intent: Intent?, context: Context) {
        clearAll()

        intent?.let {

            val action = it.action
            val type = it.type

            type?.let {
                when (action) {
                    Intent.ACTION_SEND -> {
                        when (type) {
                            "text/plain" -> {
                                // Handle text being sent
                                handleSendText(intent)
                            }
                            else -> {
                                // Handle image or files being sent
                                handleSend(intent, context)
                            }
                        }
                    }
                    Intent.ACTION_SEND_MULTIPLE -> {
                        // Handle multiple images or files being sent
                        handleSendMultiple(intent, context)
                    }
                }
            }
        }
    }

    private fun clearAll() {
        files.clear()
        sharedText = null
    }

    private fun handleSendText(intent: Intent) {
        sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
    }

    private fun handleSend(intent: Intent, context: Context) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            loadFile(context, it)
        }
    }

    private fun handleSendMultiple(intent: Intent, context: Context) {
        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
            val uris: List<Uri> = it.filterIsInstance<Uri>()
            for (uri in uris) {
                loadFile(context, uri)
            }
        }
    }

    private fun loadFile(context: Context, uri: Uri) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()

            val name = cursor.getString(nameIndex)
            val size = cursor.getLong(sizeIndex)
            val mimeType = context.contentResolver.getType(uri)
            val inputStream = context.contentResolver.openInputStream(uri)

            if (mimeType != null && inputStream != null) {
                files.add(
                    SharedFile(
                        uri, inputStream, name, mimeType, size.toInt()
                    )
                )
            }
        }
    }

    fun getFilesAsString(): Array<String> {
        return Array(files.size) {
            return@Array files[it].name
        }
    }

    fun getTextAndClear(): String {
        val text = sharedText.orEmpty()
        sharedText = null

        return text
    }

    fun getText(): String {
        return sharedText.orEmpty()
    }

    fun clear() {
        files.clear()
        sharedText = null
    }

    class SharedFile(
        var uri: Uri,
        var fis: InputStream,
        var name: String,
        val mimeType: String,
        val size: Int,
        var send: Boolean = true
    )
}