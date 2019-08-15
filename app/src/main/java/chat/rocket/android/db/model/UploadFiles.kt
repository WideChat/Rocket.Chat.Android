package chat.rocket.android.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_files")
data class UploadFileEntity(
        @PrimaryKey val id: String,
        val roomId: String,
        val fileName: String,
        val mimeType: String,
        val msg: String,
        val uri: String,
        val sent: Boolean = false,
        val retry: Boolean = true
)