package chat.rocket.android.wallet.presentation

import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.main.presentation.MainNavigator
import chat.rocket.android.server.domain.*
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import chat.rocket.android.util.extensions.launchUI
import chat.rocket.android.util.retryIO
import chat.rocket.common.RocketChatException
import chat.rocket.common.model.RoomType
import chat.rocket.common.model.Token
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.rest.me
import okhttp3.*
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class WalletPresenter @Inject constructor (private val view: WalletView,
                                           private val strategy: CancelStrategy,
                                           private val navigator: MainNavigator,
                                           private val localRepository: LocalRepository,
                                           private val getChatRoomsInteractor: GetChatRoomsInteractor,
                                           private val tokenRepository: TokenRepository,
                                           serverInteractor: GetCurrentServerInteractor,
                                           factory: RocketChatClientFactory) {

    private val serverUrl = serverInteractor.get()!!
    private val client: RocketChatClient = factory.create(serverUrl)
    private val restUrl: HttpUrl? = HttpUrl.parse(serverUrl)

    /**
     * Change the walletAddress field in the current user's customFields field
     *
     * @param address not required, but should start with "0x"
     *
     * NOTE: this function directly calls the REST API, which normally should be
     *          done in the Kotlin SDK
     */
    fun updateWalletAddress(address: String) {
        launchUI(strategy) {
            try {
                val httpUrl = restUrl?.newBuilder()
                        ?.addPathSegment("api")
                        ?.addPathSegment("v1")
                        ?.addPathSegment("users.update")
                        ?.build()

                val me = retryIO("me") { client.me() }
                val payloadBody = "{\"userId\":\"${me.id}\",\"data\":{\"customFields\":{\"walletAddress\":\"$address\"}}}"
                val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payloadBody)
                val builder = Request.Builder().url(httpUrl)

                val token: Token? = tokenRepository.get(serverUrl)
                token?.let {
                    builder.addHeader("X-Auth-Token", token.authToken)
                            .addHeader("X-User-Id", token.userId)
                }
                val request = builder.post(body).build()

                val httpClient = OkHttpClient()
                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) { Timber.d("ERROR: request call failed!")}
                    override fun onResponse(call: Call, response: Response) {
                        Timber.d("13567:: %s",(response.body()?.string()))
                        // TODO do something
                    }
                })
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    /**
     * Retrieve the walletAddress field in a user's customFields object
     *
     * @param username the user name of the user to get the walletAddress of
     *                  if none is given, then the current user is used
     *
     * NOTE: this function directly calls the REST API, which normally should be
     *          done in the Kotlin SDK
     */
    fun loadWalletAddress(username: String? = null) {
        launchUI(strategy) {
            try {
                val me = retryIO("me") { client.me() }
                val httpUrl = restUrl?.newBuilder()
                        ?.addPathSegment("api")
                        ?.addPathSegment("v1")
                        ?.addPathSegment("users.info")
                        ?.addQueryParameter("username", username ?: me.username)
                        ?.build()

                val token: Token? = tokenRepository.get(serverUrl)
                val builder = Request.Builder().url(httpUrl)
                token?.let {
                    builder.addHeader("X-Auth-Token", token.authToken)
                            .addHeader("X-User-Id", token.userId)
                }

                val request = builder.get().build()
                val httpClient = OkHttpClient()
                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) { Timber.d("ERROR: request call failed!")}
                    override fun onResponse(call: Call, response: Response) {
                        var jsonObject = JSONObject(response.body()?.string())
                        if (jsonObject.isNull("error")) {

                            if (!jsonObject.isNull("user")) {
                                jsonObject = jsonObject.getJSONObject("user")

                                if (!jsonObject.isNull("customFields")) {
                                    jsonObject = jsonObject.getJSONObject("customFields")
                                    val walletAddress = jsonObject.getString("walletAddress")
                                    // TODO do something
                                }
                            }
                        } else {
                            Timber.d("ERROR: %s", jsonObject.getString("error"))
                        }
                    }
                })
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun getUserName(): String {
        return localRepository.get(LocalRepository.CURRENT_USERNAME_KEY) ?: ""
    }

    fun loadDMRoomByName(name: String) {
        launchUI(strategy) {
            try {
                val roomList = getChatRoomsInteractor.getByName(serverUrl, name)
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