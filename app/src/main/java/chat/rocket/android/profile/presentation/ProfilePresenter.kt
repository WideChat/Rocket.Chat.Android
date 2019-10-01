package chat.rocket.android.profile.presentation

import android.graphics.Bitmap
import android.net.Uri
import chat.rocket.android.chatroom.domain.UriInteractor
import chat.rocket.android.core.behaviours.showMessage
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.db.DatabaseManagerFactory
import chat.rocket.android.helper.UserHelper
import chat.rocket.android.main.presentation.MainNavigator
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import chat.rocket.android.server.domain.RemoveAccountInteractor
import chat.rocket.android.server.domain.TokenRepository
import chat.rocket.android.server.infraestructure.ConnectionManagerFactory
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import chat.rocket.android.server.presentation.CheckServerPresenter
import chat.rocket.android.util.extension.compressImageAndGetByteArray
import chat.rocket.android.util.extension.gethash
import chat.rocket.android.util.extension.launchUI
import chat.rocket.android.util.extension.toHex
import chat.rocket.android.util.extensions.avatarUrl
import chat.rocket.android.util.retryIO
import chat.rocket.common.RocketChatException
import chat.rocket.common.util.ifNull
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.rest.deleteOwnAccount
import chat.rocket.core.internal.rest.resetAvatar
import chat.rocket.core.internal.rest.setAvatar
import chat.rocket.core.internal.rest.updateProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

// WIDECHAT
import android.content.Context
import android.provider.MediaStore
import chat.rocket.core.internal.rest.getAccessToken
import chat.rocket.android.server.domain.GetSettingsInteractor
import chat.rocket.android.server.domain.RefreshSettingsInteractor
import chat.rocket.android.util.extensions.encodeToBase64
import chat.rocket.core.model.Myself
import kotlinx.coroutines.channels.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType
import okhttp3.Protocol

class ProfilePresenter @Inject constructor(
    private val view: ProfileView,
    private val strategy: CancelStrategy,
    private val uriInteractor: UriInteractor,
    val userHelper: UserHelper,
    navigator: MainNavigator,
    serverInteractor: GetCurrentServerInteractor,
    refreshSettingsInteractor: RefreshSettingsInteractor,
    settingsInteractor: GetSettingsInteractor,
    factory: RocketChatClientFactory,
    removeAccountInteractor: RemoveAccountInteractor,
    tokenRepository: TokenRepository,
    dbManagerFactory: DatabaseManagerFactory,
    managerFactory: ConnectionManagerFactory
) : CheckServerPresenter(
    strategy = strategy,
    factory = factory,
    serverInteractor = serverInteractor,
    settingsInteractor = settingsInteractor,
    refreshSettingsInteractor = refreshSettingsInteractor,
    removeAccountInteractor = removeAccountInteractor,
    tokenRepository = tokenRepository,
    dbManagerFactory = dbManagerFactory,
    managerFactory = managerFactory,
    tokenView = view,
    navigator = navigator
) {
    private val serverUrl = serverInteractor.get()!!
    private val client: RocketChatClient = factory.create(serverUrl)
    private val user = userHelper.user()
    private val userDataChannel = Channel<Myself>()
    private val currentServer = serverInteractor.get()!!

    // WIDECHAT
    private var currentAccessToken: String? = null


    fun loadUserProfile() {
        launchUI(strategy) {
            view.showLoading()
            try {
                view.showProfile(
                    serverUrl.avatarUrl(user?.username ?: ""),
                    user?.name ?: "",
                    user?.username ?: "",
                    user?.emails?.getOrNull(0)?.address ?: "",
                    user?.telephoneNumber ?: ""
                )
            } catch (exception: RocketChatException) {
                view.showMessage(exception)
            } finally {
                view.hideLoading()
            }
        }
    }

    // WIDECHAT - profile update with SSO
    fun setUpdateUrl(updatePath: String?, onClickCallback: (String?) -> Unit?) {
        launchUI(strategy) {
            try {
                withContext(Dispatchers.Default) {
                    setupConnectionInfo(serverUrl)
                    refreshServerAccounts()
                    checkEnabledAccounts(serverUrl)
                }
                retryIO { currentAccessToken = client.getAccessToken(customOauthServiceName.toString()) }
                val tokenInfo = "{\"userid\":\"${user?.username}\",\"telephoneNumber\":\"${user?.telephoneNumber}\",\"email\":\"${user?.emails?.getOrNull(0)?.address ?: ""}\"}"
                onClickCallback("${customOauthHost}${updatePath}access_token=${currentAccessToken}&token_info=${tokenInfo.encodeToBase64()}")
            } catch (ex: Exception) {
                view.showMessage(ex)
            }
        }
    }

    fun updateUserProfile(email: String, name: String, username: String) {
        launchUI(strategy) {
            view.showLoading()
            try {
                user?.id?.let { id ->
                    retryIO { client.updateProfile(userId = id, email = email, name = name, username = username) }
                    view.showProfileUpdateSuccessfullyMessage()
                    view.showProfile(
                        serverUrl.avatarUrl(user.username ?: ""),
                        name,
                        username,
                        email
                    )
                }
            } catch (exception: RocketChatException) {
                exception.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            } finally {
                view.hideLoading()
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        launchUI(strategy) {
            view.showLoading()
            try {
                retryIO {
                    client.setAvatar(
                        uriInteractor.getFileName(uri) ?: uri.toString(),
                        uriInteractor.getMimeType(uri)
                    ) {
                        uriInteractor.getInputStream(uri)
                    }
                }
                user?.username?.let { view.reloadUserAvatar(serverUrl.avatarUrl(it)) }
            } catch (exception: RocketChatException) {
                exception.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            } finally {
                view.hideLoading()
            }
        }
    }

    // WIDECHAT
    fun prepareFileAndUpdateAvatar(uri: Uri, context: Context?) {
        val bitmap = MediaStore.Images.Media.getBitmap(context?.getContentResolver(), uri)
        preparePhotoAndUpdateAvatar(bitmap)
    }

    fun preparePhotoAndUpdateAvatar(bitmap: Bitmap) {
        launchUI(strategy) {
            view.showLoading()
            try {
                val byteArray = bitmap.compressImageAndGetByteArray("image/png")

                retryIO {
                    client.setAvatar(
                        UUID.randomUUID().toString() + ".png",
                        "image/png"
                    ) {
                        byteArray?.inputStream()
                    }
                }

                user?.username?.let { view.reloadUserAvatar(serverUrl.avatarUrl(it)) }
            } catch (exception: RocketChatException) {
                exception.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            } finally {
                view.hideLoading()
            }
        }
    }

    fun resetAvatar() {
        launchUI(strategy) {
            view.showLoading()
            try {
                user?.id?.let { id ->
                    retryIO { client.resetAvatar(id) }
                }
                user?.username?.let { view.reloadUserAvatar(serverUrl.avatarUrl(it)) }
            } catch (exception: RocketChatException) {
                exception.message?.let {
                    view.showMessage(it)
                }.ifNull {
                    view.showGenericErrorMessage()
                }
            } finally {
                view.hideLoading()
            }
        }
    }


    fun logout() {
        setupConnectionInfo(currentServer)
        super.logout(userDataChannel)
    }


}
