package chat.rocket.android.chatrooms.presentation

import chat.rocket.android.R
import chat.rocket.android.chatrooms.adapter.model.RoomUiModel
import chat.rocket.android.chatrooms.domain.FetchChatRoomsInteractor
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.db.DatabaseManager
import chat.rocket.android.db.model.ChatRoomEntity
import chat.rocket.android.helper.UserHelper
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.main.presentation.MainNavigator
import chat.rocket.android.server.domain.SettingsRepository
import chat.rocket.android.server.domain.useRealName
import chat.rocket.android.server.domain.useSpecialCharsOnRoom
import chat.rocket.android.server.infraestructure.ConnectionManager
import chat.rocket.android.util.extension.launchUI
import chat.rocket.android.util.retryDB
import chat.rocket.android.util.retryIO
import chat.rocket.common.RocketChatException
import chat.rocket.common.model.Email
import chat.rocket.common.model.RoomType
import chat.rocket.common.model.User
import chat.rocket.common.model.roomTypeOf
import chat.rocket.core.internal.realtime.createDirectMessage
import chat.rocket.core.internal.rest.me
import chat.rocket.core.internal.rest.show
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// WIDECHAT
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity.WIFI_SERVICE
import androidx.fragment.app.FragmentActivity
import chat.rocket.android.helper.AndroidPermissionsHelper
import chat.rocket.android.helper.Constants
import chat.rocket.android.helper.SharedPreferenceHelper
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.sharehadler.ShareHandler

class ChatRoomsPresenter @Inject constructor(
    private val view: ChatRoomsView,
    private val strategy: CancelStrategy,
    private val navigator: MainNavigator,
    @Named("currentServer") private val currentServer: String,
    private val dbManager: DatabaseManager,
    manager: ConnectionManager,
    private val localRepository: LocalRepository,
    private val userHelper: UserHelper,
    settingsRepository: SettingsRepository
) {
    private val client = manager.client
    private val settings = settingsRepository.get(currentServer)

    fun loadChatRoom(roomId: String) {
        launchUI(strategy) {
            view.showLoadingRoom("")
            try {
                val room = dbManager.getRoom(roomId)
                if (room != null) {
                    loadChatRoom(room.chatRoom, true)
                } else {
                    Timber.d("Error loading channel")
                    view.showGenericErrorMessage()
                }
            } catch (ex: Exception) {
                Timber.d(ex, "Error loading channel")
                view.showGenericErrorMessage()
            } finally {
                view.hideLoadingRoom()
            }
        }
    }

    fun loadChatRoom(chatRoom: RoomUiModel) {
        launchUI(strategy) {
            view.showLoadingRoom(chatRoom.name)
            try {
                val room = retryDB("getRoom(${chatRoom.id}") { dbManager.getRoom(chatRoom.id) }
                if (room != null) {
                    loadChatRoom(room.chatRoom, true)
                } else {
                    with(chatRoom) {
                        val entity = ChatRoomEntity(
                            id = id,
                            subscriptionId = "",
                            type = type.toString(),
                            name = username ?: name.toString(),
                            fullname = name.toString(),
                            open = open,
                            muted = muted
                        )
                        loadChatRoom(entity, false)
                    }
                }
            } catch (ex: Exception) {
                Timber.d(ex, "Error loading channel")
                view.showGenericErrorMessage()
            } finally {
                view.hideLoadingRoom()
            }
        }
    }

    suspend fun loadChatRoom(chatRoom: ChatRoomEntity, local: Boolean = false) {
        with(chatRoom) {
            val isDirectMessage = roomTypeOf(type) is RoomType.DirectMessage
            val roomName = if (settings.useSpecialCharsOnRoom() || (isDirectMessage && settings.useRealName())) {
                fullname ?: name
            } else {
                name
            }

            val myself = getCurrentUser()
            if (myself?.username == null) {
                view.showMessage(R.string.msg_generic_error)
            } else {

                // todo CHECK
                if (ShareHandler.hasShare() && (readonly != null && readonly!!)) {
                    view.showMessage("You cannot send message to readonly channel")

                    return@with
                }

                val id = if (isDirectMessage && !open) {
                    // If from local database, we already have the roomId, no need to concatenate
                    if (local) {
                        retryIO {
                            client.show(id, roomTypeOf(RoomType.DIRECT_MESSAGE))
                        }
                        id
                    } else {
                        retryIO("createDirectMessage($name)") {
                            withTimeout(10000) {
                                createDirectMessage(name)
                                FetchChatRoomsInteractor(client, dbManager).refreshChatRooms()
                            }
                        }
                        val fromTo = mutableListOf(myself.id, id).apply {
                            sort()
                        }
                        fromTo.joinToString("")
                    }
                } else {
                    id
                }

                navigator.toChatRoom(
                        chatRoomId = id,
                        chatRoomName = roomName,
                        chatRoomType = type,
                        isReadOnly = readonly ?: false,
                        chatRoomLastSeen = lastSeen ?: -1,
                        isSubscribed = open,
                        isCreator = ownerId == myself.id || isDirectMessage,
                        isFavorite = favorite ?: false
                )
            }
        }
    }

    suspend fun getCurrentUser(local: Boolean = true): User? {
        if (local) {
            userHelper.user()?.let {
                return it
            }
        }
        try {
            val myself = retryIO { client.me() }

            // Cast the email object to the correct data class
            var emailObj = myself.emails?.getOrNull(0)
            var emails: List<Email> = listOf()
            if (emailObj != null) {
                emails = listOf(Email(emailObj.address.toString(), emailObj.verified))
            }

            val user = User(
                id = myself.id,
                username = myself.username,
                name = myself.name,
                status = myself.status,
                utcOffset = myself.utcOffset,
                emails = emails,
                roles = myself.roles,
                telephoneNumber = myself.telephoneNumber
            )

            localRepository.saveCurrentUser(url = currentServer, user = user)
            return user
        } catch (ex: RocketChatException) {
            Timber.e(ex)
        }
        return null
    }

    private suspend fun createDirectMessage(name: String): Boolean = suspendCoroutine { cont ->
        client.createDirectMessage(name) { success, _ ->
            cont.resume(success)
        }
    }

    fun canShareToRoom(room: RoomUiModel, onAllowed: () -> Unit, onDisallowed: () -> Unit) {
        launchUI(strategy) {
            dbManager.getRoom(room.id)?.apply {
                val isReadonly = this.chatRoom.readonly

                isReadonly?.let { isReadOnly ->
                    if (isReadOnly) {
                        onDisallowed()
                    } else {
                        onAllowed()
                    }
                }
            }
        }
    }

    // WIDECHAT
    fun tryToReadSSID(activity: FragmentActivity?) {
        with(activity as MainActivity) {
            if ((AndroidPermissionsHelper.hasLocationPermission(this)) or
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1)) {
                val wifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.getConnectionInfo()

                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    val ssid: String? = wifiInfo.getBSSID()
                    ssid?.let {
                        SharedPreferenceHelper.putString(Constants.CURRENT_BSSID, it)
                        Timber.d("Current bssid is: ${ssid}")
                    }
                } else {
                    SharedPreferenceHelper.putString(Constants.CURRENT_BSSID, "none")
                }
            } else {
                // Clear the value in case permissions revoked
                SharedPreferenceHelper.putString(Constants.CURRENT_BSSID, "none")
            }
        }
    }
}