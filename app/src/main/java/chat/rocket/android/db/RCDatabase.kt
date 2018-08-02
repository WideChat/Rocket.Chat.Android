package chat.rocket.android.db

import androidx.room.Database
import androidx.room.RoomDatabase
import chat.rocket.android.db.model.ChatRoomEntity
import chat.rocket.android.db.model.UserEntity
import chat.rocket.android.room.weblink.WebLinkDao
import chat.rocket.android.room.weblink.WebLinkEntity

@Database(
        entities = [UserEntity::class, ChatRoomEntity::class, WebLinkEntity::class],
        version = 3,
        exportSchema = true
)
abstract class RCDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun chatRoomDao(): ChatRoomDao

    abstract fun webLinkDao(): WebLinkDao
}