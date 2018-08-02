package chat.rocket.android.room.weblink

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "weblink", indices = [(Index(value = ["link"], unique = true))])
data class WebLinkEntity(
        val title: String = "",
        val description: String = "",
        val imageUrl: String = "",
        @PrimaryKey
        val link: String
)
