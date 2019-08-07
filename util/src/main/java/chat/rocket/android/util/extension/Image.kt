package chat.rocket.android.util.extension

import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compress a [Bitmap] image.
 *
 * @param mimeType The MimeType of what the compressed image should be.
 * @return An [InputStream] of a compressed image, otherwise null if the compression couldn't be done.
 */
suspend fun Bitmap.compressImageAndGetInputStream(mimeType: String): InputStream? {
    var inputStream: InputStream? = null

    withContext(Dispatchers.Default) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // TODO: Add an option the the app to the user be able to select the quality of the compressed image
        val isCompressed = compress(mimeType.getCompressFormat(), 70, byteArrayOutputStream)
        if (isCompressed) {
            inputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        }
    }

    return inputStream
}


/**
 * Returns a [ByteArray] of a [Bitmap].
 *
 * @param mimeType The MIME type of the [Bitmap].
 * @param quality The quality of the [Bitmap] for the resulting [ByteArray].
 * @param maxFileSizeAllowed The max file size allowed by the server. Note: The [quality] will be
 * decreased minus 20 until the [ByteArray] size fits the [maxFileSizeAllowed] value.
 * @return A [ByteArray] of a [Bitmap]
 */
suspend fun Bitmap.getByteArray(
    mimeType: String,
    quality: Int,
    maxFileSizeAllowed: Int
): ByteArray {
    lateinit var byteArray: ByteArray

    compressImageAndGetByteArray(mimeType, quality)?.let {
        if (it.size > maxFileSizeAllowed && maxFileSizeAllowed !in -1..0) {
            if (quality == 0 || !mimeType.lossyCompressible()) {
                throw InvalidObjectException ("File size too big.")
            }
            // call this method recursively with lower quality
            byteArray = getByteArray(mimeType, quality - 20, maxFileSizeAllowed)
        } else {
            byteArray = it
        }
    }

    return byteArray
}

/**
 * Compress a [Bitmap] image.
 *
 * @param mimeType The MimeType of what the compressed image should be.
 * @return An [ByteArray] of a compressed image, otherwise null if the compression couldn't be done.
 */
suspend fun Bitmap.compressImageAndGetByteArray(mimeType: String, quality: Int = 100): ByteArray? {
    var byteArray: ByteArray? = null

    withContext(Dispatchers.Default) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val isCompressed = compress(mimeType.getCompressFormat(), quality, byteArrayOutputStream)
        if (isCompressed) {
            byteArray = byteArrayOutputStream.toByteArray()
        }
    }

    return byteArray
}

/**
 * Gets the [Bitmap.CompressFormat] based on the image MimeType.
 * Note: Supported formats are: PNG, JPEG and WEBP.
 */
fun String.getCompressFormat(): Bitmap.CompressFormat {
    return when {
        this.contains("jpeg") -> Bitmap.CompressFormat.JPEG
        this.contains("webp") -> Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.PNG
    }
}

/**
 * Returns true if lossy compressible format based on mimeType
 */
fun String.lossyCompressible(): Boolean {
    return when {
        this.contains("jpeg") -> true
        this.contains("png") -> true
        this.contains("webp") -> true
        else -> false
    }
}

fun Fragment.dispatchImageSelection(requestCode: Int) {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.type = "image/*"
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    startActivityForResult(intent, requestCode)
}

fun Fragment.dispatchTakePicture(requestCode: Int) {
    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    if (takePictureIntent.resolveActivity(context?.packageManager) != null) {
        startActivityForResult(takePictureIntent, requestCode)
    }
}

@Throws(IOException::class)
fun FragmentActivity.createImageFile(): File {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val storageDir: File =  getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "PNG_${timeStamp}_", /* prefix */
        ".png", /* suffix */
        storageDir /* directory */
    )
}