package chat.rocket.android.wallet.presentation

import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.main.presentation.MainNavigator
import chat.rocket.android.server.domain.GetChatRoomsInteractor
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import chat.rocket.android.util.extensions.launchUI
import chat.rocket.common.RocketChatException
import chat.rocket.common.model.RoomType
import timber.log.Timber
import javax.inject.Inject

class WalletPresenter @Inject constructor (private val view: WalletView,
                                           private val strategy: CancelStrategy,
                                           private val navigator: MainNavigator,
                                           private val localRepository: LocalRepository,
                                           private val serverInteractor: GetCurrentServerInteractor,
                                           private val getChatRoomsInteractor: GetChatRoomsInteractor) {

    fun getUserName(): String {
        return localRepository.get(LocalRepository.CURRENT_USERNAME_KEY) ?: ""
    }

    fun loadDMRoomByName(name: String) {
        val currentServer = serverInteractor.get()!!
        launchUI(strategy) {
            try {
                val roomList = getChatRoomsInteractor.getByName(currentServer, name)
                val directMessageRoomList = roomList.filter( {it.type.javaClass == RoomType.DIRECT_MESSAGE.javaClass} )

                if (directMessageRoomList.isEmpty()) {
                    view.showRoomFailedToLoadMessage(name)
                } else {
                    val room = directMessageRoomList.first()
                    navigator.toChatRoom(room.id, room.name, room.type.toString(),
                            room.readonly ?: false,
                            room.lastSeen ?: -1,
                            room.open, true)
                }
            } catch (ex: RocketChatException) {
                Timber.e(ex)
            }
        }
    }
}