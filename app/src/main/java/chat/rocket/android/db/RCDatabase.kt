package chat.rocket.android.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import chat.rocket.android.db.model.ChatRoomEntity
import chat.rocket.android.db.model.UserEntity
import chat.rocket.android.room.weblink.WebLinkDao
import chat.rocket.android.room.weblink.WebLinkEntity

@Database(
    entities = [UserEntity::class, ChatRoomEntity::class, WebLinkEntity::class],
    version = 5,
    exportSchema = true
)

abstract class RCDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun chatRoomDao(): ChatRoomDao

    abstract fun webLinkDao(): WebLinkDao

    companion object {
        @JvmField
        val MIGRATION_4_5 = Migration4to5()
    }
}

class Migration4to5 : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX `index_chatrooms_lastMessageUserId` ON `chatrooms` (`lastMessageUserId`)")
    }
}