package chat.rocket.android.main.presentation

import android.content.Context
import chat.rocket.android.authentication.domain.model.DeepLinkInfo
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.db.DatabaseManagerFactory
import chat.rocket.android.emoji.Emoji
import chat.rocket.android.emoji.EmojiRepository
import chat.rocket.android.emoji.Fitzpatrick
import chat.rocket.android.emoji.internal.EmojiCategory
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.main.uimodel.NavHeaderUiModel
import chat.rocket.android.main.uimodel.NavHeaderUiModelMapper
import chat.rocket.android.push.GroupedPush
import chat.rocket.android.server.domain.GetAccountsInteractor
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import chat.rocket.android.server.domain.GetSettingsInteractor
import chat.rocket.android.server.domain.PublicSettings
import chat.rocket.android.server.domain.RefreshSettingsInteractor
import chat.rocket.android.server.domain.RefreshPermissionsInteractor
import chat.rocket.android.server.domain.RemoveAccountInteractor
import chat.rocket.android.server.domain.SaveAccountInteractor
import chat.rocket.android.server.domain.TokenRepository
import chat.rocket.android.server.domain.favicon
import chat.rocket.android.server.domain.model.Account
import chat.rocket.android.server.infraestructure.ConnectionManagerFactory
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import chat.rocket.android.server.presentation.CheckServerPresenter
import chat.rocket.android.util.extension.launchUI
import chat.rocket.android.util.extensions.adminPanelUrl
import chat.rocket.android.util.extensions.serverLogoUrl
import chat.rocket.android.util.retryIO
import chat.rocket.common.RocketChatAuthException
import chat.rocket.common.RocketChatException
import chat.rocket.common.model.UserStatus
import chat.rocket.common.util.ifNull
import chat.rocket.core.RocketChatClient
import chat.rocket.core.model.Myself
import chat.rocket.core.internal.rest.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import javax.inject.Inject

// WIDECHAT
import androidx.core.net.toUri
import com.facebook.drawee.backends.pipeline.Fresco
import chat.rocket.android.infrastructure.username
import chat.rocket.android.util.extension.gethash
import chat.rocket.android.util.extension.toHex
import chat.rocket.android.util.extensions.avatarUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.util.*

class MainPresenter @Inject constructor(
    private val view: MainView,
    private val strategy: CancelStrategy,
    private val navigator: MainNavigator,
    private val tokenRepository: TokenRepository,
    private val refreshSettingsInteractor: RefreshSettingsInteractor,
    private val refreshPermissionsInteractor: RefreshPermissionsInteractor,
    private val navHeaderMapper: NavHeaderUiModelMapper,
    private val saveAccountInteractor: SaveAccountInteractor,
    private val getAccountsInteractor: GetAccountsInteractor,
    private val groupedPush: GroupedPush,
    serverInteractor: GetCurrentServerInteractor,
    localRepository: LocalRepository,
    removeAccountInteractor: RemoveAccountInteractor,
    factory: RocketChatClientFactory,
    dbManagerFactory: DatabaseManagerFactory,
    getSettingsInteractor: GetSettingsInteractor,
    managerFactory: ConnectionManagerFactory
) : CheckServerPresenter(
    strategy = strategy,
    factory = factory,
    serverInteractor = serverInteractor,
    localRepository = localRepository,
    removeAccountInteractor = removeAccountInteractor,
    tokenRepository = tokenRepository,
    managerFactory = managerFactory,
    dbManagerFactory = dbManagerFactory,
    tokenView = view,
    navigator = navigator
) {
    private val currentServer = serverInteractor.get()!!
    private val manager = managerFactory.create(currentServer)
    private val client: RocketChatClient = factory.create(currentServer)
    private var settings: PublicSettings = getSettingsInteractor.get(serverInteractor.get()!!)
    private val userDataChannel = Channel<Myself>()
    //commented out because we are not using the sso api call because the delete account button only deletes the account from the RC server rather than the SSO account
    //private val ssoApiClient = OkHttpClient().newBuilder().protocols(Arrays.asList(Protocol.HTTP_1_1))

    // WIDECHAT
    private val currentUsername = localRepository.username()

    fun toChatList(chatRoomId: String? = null, deepLinkInfo: DeepLinkInfo? = null) = navigator.toChatList(chatRoomId, deepLinkInfo)

    fun toUserProfile() = navigator.toUserProfile()

    fun toSettings() = navigator.toSettings()

    fun toAdminPanel() = tokenRepository.get(currentServer)?.let {
        navigator.toAdminPanel(currentServer.adminPanelUrl(), it.authToken)
    }

    fun toCreateChannel() = navigator.toCreateChannel()

    fun loadServerAccounts() {
        launchUI(strategy) {
            try {
                view.setupServerAccountList(getAccountsInteractor.get())
            } catch (ex: Exception) {
                when (ex) {
                    is RocketChatAuthException -> logout()
                    else -> {
                        Timber.d(ex, "Error loading serve accounts")
                        ex.message?.let {
                            view.showMessage(it)
                        }.ifNull {
                            view.showGenericErrorMessage()
                        }
                    }
                }
            }
        }
    }

    fun clearAvatarUrlFromCache() {
        val myAvatarUrl: String? =  currentServer.avatarUrl(currentUsername ?: "")
        Fresco.getImagePipeline().evictFromCache(myAvatarUrl?.toUri())
    }

    fun loadCurrentInfo() {
        setupConnectionInfo(currentServer)
        checkServerInfo(currentServer)
        launchUI(strategy) {
            try {
                val me = retryIO("me") { client.me() }
                val model = navHeaderMapper.mapToUiModel(me)
                saveAccount(model)
                view.setupUserAccountInfo(model)
            } catch (ex: Exception) {
                when (ex) {
                    is RocketChatAuthException -> logout()
                    else -> {
                        Timber.d(ex, "Error loading my information for navheader")
                        ex.message?.let {
                            view.showMessage(it)
                        }.ifNull {
                            view.showGenericErrorMessage()
                        }
                    }
                }
            }
            subscribeMyselfUpdates()
        }
    }

    /**
     * Load all emojis for the current server. Simple emojis are always the same for every server,
     * but custom emojis vary according to the its url.
     */
    fun loadEmojis() {
        launchUI(strategy) {
            EmojiRepository.setCurrentServerUrl(currentServer)
            val customEmojiList = mutableListOf<Emoji>()
            try {
                for (customEmoji in retryIO("getCustomEmojis()") { client.getCustomEmojis() }) {
                    customEmojiList.add(Emoji(
                        shortname = ":${customEmoji.name}:",
                        category = EmojiCategory.CUSTOM.name,
                        url = "$currentServer/emoji-custom/${customEmoji.name}.${customEmoji.extension}",
                        count = 0,
                        fitzpatrick = Fitzpatrick.Default.type,
                        keywords = customEmoji.aliases,
                        shortnameAlternates = customEmoji.aliases,
                        siblings = mutableListOf(),
                        unicode = "",
                        isDefault = true
                    ))
                }

                EmojiRepository.load(view as Context, customEmojis = customEmojiList)
            } catch (ex: RocketChatException) {
                Timber.e(ex)
                EmojiRepository.load(view as Context)
            }
        }
    }

    fun logout() {
        setupConnectionInfo(currentServer)
        super.logout(userDataChannel)
    }

    fun connect() {
        refreshSettingsInteractor.refreshAsync(currentServer)
        refreshPermissionsInteractor.refreshAsync(currentServer)
        manager.connect()
    }

    fun disconnect() {
        setupConnectionInfo(currentServer)
        super.disconnect(userDataChannel)
    }

    fun changeServer(serverUrl: String) {
        if (currentServer != serverUrl) {
            navigator.switchOrAddNewServer(serverUrl)
        } else {
            view.closeServerSelection()
        }
    }

    fun addNewServer() {
        navigator.toServerScreen()
    }

    fun changeDefaultStatus(userStatus: UserStatus) {
        launchUI(strategy) {
            try {
                manager.setDefaultStatus(userStatus)
                view.showUserStatus(userStatus)
            } catch (ex: RocketChatException) {
                ex.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            }
        }
    }

    private fun saveAccount(uiModel: NavHeaderUiModel) {
        val icon = settings.favicon()?.let {
            currentServer.serverLogoUrl(it)
        }
        val account = Account(
            currentServer,
            icon,
            uiModel.serverLogo,
            uiModel.userDisplayName!!,
            uiModel.userAvatar
        )
        saveAccountInteractor.save(account)
    }

    private suspend fun subscribeMyselfUpdates() {
        manager.addUserDataChannel(userDataChannel)
        for (myself in userDataChannel) {
            updateMyself(myself)
        }
    }

    private fun updateMyself(myself: Myself) =
        view.setupUserAccountInfo(navHeaderMapper.mapToUiModel(myself))

    fun clearNotificationsForChatroom(chatRoomId: String?) {
        if (chatRoomId == null) return

        groupedPush.hostToPushMessageList[currentServer]?.let { list ->
            list.removeAll { it.info.roomId == chatRoomId }
        }
    }

    // WIDECHAT
    fun widechatDeleteAccount(username: String, ssoDeleteCallback: () -> Unit?) {
        launchUI(strategy) {
            //view.showLoading()
            try {
                withContext(Dispatchers.Default) {
                    retryIO { client.deleteOwnAccount(username) }
                    //commented out the line below because delete account functionality does not work
                    //this requires a discussion about if we want the user to be able to delete their SSO account or just VC account
                    //ssoDeleteCallback()
                    setupConnectionInfo(currentServer)
                    logout(null)
                }
            } catch (exception: Exception) {
                exception.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            } finally {
                //view.hideLoading()
            }
        }
    }

    //this function is broken because the currentAccessToken is not being accessed properly
    // TODO: Is it neccessary to move this into the Kotlin SDK?
    fun widechatDeleteSsoAccount(ssoProfileDeletePath: String?) {
        var currentAccessToken: String? = null

        val MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8")
        val json = """{"profilemap":{"username":"userid"}}""".trimIndent()

        launchUI(strategy) {
            retryIO { currentAccessToken = client.getAccessToken(customOauthServiceName.toString()) }

            var request: Request = Request.Builder()
                    .url("${customOauthHost}${ssoProfileDeletePath}")
                    .delete(RequestBody.create(MEDIA_TYPE_JSON, json))
                    .addHeader("Authorization", "Bearer ${currentAccessToken}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("cache-control", "no-cache")
                    .build()

            // TODO: Implement validation check? What action upon failure?
            //commented out because we are not using the sso api call because the delete account button only deletes the account from the RC server rather than the SSO account
            //val response = ssoApiClient.build().newCall(request).execute()
        }
    }

    fun deleteAccount(password: String) {
        launchUI(strategy) {
            //view.showLoading()
            try {
                withContext(Dispatchers.Default) {
                    // REMARK: Backend API is only working with a lowercase hash.
                    // https://github.com/RocketChat/Rocket.Chat/issues/12573
                    retryIO { client.deleteOwnAccount(password.gethash().toHex().toLowerCase()) }
                    setupConnectionInfo(currentServer)
                    logout(null)
                }
            } catch (exception: Exception) {
                exception.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            } finally {
                //view.hideLoading()
            }
        }
    }

}
