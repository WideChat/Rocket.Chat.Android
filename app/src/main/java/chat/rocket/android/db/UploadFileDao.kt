package chat.rocket.android.db

import androidx.room.*
import chat.rocket.android.db.model.UploadFileEntity

@Dao
abstract class UploadFileDao : BaseDao<UploadFileEntity> {
    @Transaction
    @Query("""
        SELECT * FROM upload_files
    """)
    abstract fun getAllSync(): List<UploadFileEntity>

    @Insert
    abstract fun insert(uploadFile: UploadFileEntity)

    @Transaction
    @Query("""
        SELECT * FROM upload_files WHERE sent = 0 AND retry = 1
    """)
    abstract fun getAllUnsent(): List<UploadFileEntity>

    @Update
    abstract fun update(list: List<UploadFileEntity>)

    @Update
    abstract fun update(uploadFile: UploadFileEntity)

    @Delete
    abstract fun delete(uploadFile: UploadFileEntity)
}