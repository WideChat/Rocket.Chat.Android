package chat.rocket.android.wallet.presentation

import android.content.Context
import android.view.View
import android.widget.Toast
import chat.rocket.android.R
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.main.presentation.MainNavigator
import chat.rocket.android.server.domain.*
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import chat.rocket.android.util.extensions.launchUI
import chat.rocket.android.util.retryIO
import chat.rocket.android.wallet.BlockchainInterface
import chat.rocket.android.wallet.WalletDBInterface
import chat.rocket.common.RocketChatException
import chat.rocket.common.model.RoomType
import chat.rocket.common.model.Token
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.rest.me
import kotlinx.coroutines.experimental.async
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
                                           settingsRepository: SettingsRepository,
                                           serverInteractor: GetCurrentServerInteractor,
                                           factory: RocketChatClientFactory) {

    private val serverUrl = serverInteractor.get()!!
    private val client: RocketChatClient = factory.create(serverUrl)
    private val restUrl: HttpUrl? = HttpUrl.parse(serverUrl)
    private val bcInterface = BlockchainInterface()
    private val dbInterface = WalletDBInterface()
    private val settings = settingsRepository.get(serverUrl)
    private val managedMode = settings.isWalletManaged()


    /**
     * Get transaction history associated with the user's wallet
     */
    private fun loadTransactions(address: String) {
        launchUI(strategy) {
            try {
                // Query the DB for transaction hashes
                if (bcInterface.isValidAddress(address)) {
                    dbInterface.getTransactionList(address) { hashList ->
                        // Update transaction history
                        if (hashList != null) {
                            async {
                                view.updateTransactions(bcInterface.getTransactions(address, hashList))
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    /**
     * Unmanaged mode: Check if the user has a wallet
     *  both tied to their rocket.chat account and stored on their device
     *  and display either their wallet or the create wallet button.
     *  Only display the wallet if it is stored with the rocket.chat account and on
     *  the user's device. TODO add more options for checking for wallets (e.g. what if there's a private key file on the device, but no address in the rocket.chat account)
     *
     * Managed mode: Check if the user has a wallet in the dynamoDB database.
     *  If not, a wallet is auto-created and wallet information is added to the database.
     */
    fun loadWallet(c: Context) {

        launchUI(strategy) {
            view.showLoading()

            if ( managedMode ){    // Managed wallet

                try{
                    dbInterface.findWallet(getUserName()) { wallet ->    // Check if user has a wallet (in the database)
                        var walletAddress: String
                        if (wallet != null) {
                            walletAddress = wallet.walletAddress
                        } else { // Create a wallet for the user

                            //TODO creating a wallet takes some time, don't show button, but should there be an intermediate/loading screen?

                            val walletInfo = bcInterface.createBip39Wallet(managedMode, "", c)

                            //returns String[]{address, mnemonic, privateKey.toString(), publicKey.toString() , password}
                            val userId = getUserName()
                            val balance = 0.0
                            val mnemonic = walletInfo[1]
                            val password = walletInfo[4]
                            val privateKey = walletInfo[2]
                            val publicKey = walletInfo[3]
                            walletAddress = walletInfo[0]

                            // Save walletAddress to RC profile so outside REST API calls can access it
                            updateWalletAddress(walletAddress)

                            dbInterface.createWallet(userId, balance, mnemonic, password, privateKey, publicKey, walletAddress) {
                                Toast.makeText(c, R.string.wallet_creation_success, Toast.LENGTH_LONG).show()
                            }
                        }

                        view.showWallet(true, bcInterface.getBalance(walletAddress).toDouble())
                        loadTransactions(walletAddress)
                        view.hideLoading()
                    }

                } catch (ex: Exception){
                    Timber.e(ex)
                    view.hideLoading()
                }
            }
            else{   // Un-managed wallet

                try {
                    loadWalletAddress {
                        if (bcInterface.isValidAddress(it) && bcInterface.walletFileExists(it, c)) {
                            view.showWallet(true, bcInterface.getBalance(it).toDouble())
                            loadTransactions(it)
                        } else {
                            view.showWallet(false)
                        }
                        view.hideLoading()
                    }
                } catch (ex: Exception) {
                    view.showWallet(false)
                    view.hideLoading()
                    Timber.e(ex)
                }
            }//end of else
        }
    }

    /**
     * Unmanaged mode: Retrieve the walletAddress field in a user's customFields object
     *
     * Managed mode: Retrieve the user's walletAddress stored in the database
     *
     * If the user does not have a wallet address stored, then an empty string
     *  is given to the callback
     *
     * @param username the user name of the user to get the walletAddress of
     *                  if none is given, then the current user is used
     *
     * NOTE: this function directly calls the REST API, which normally should be
     *          done in the Kotlin SDK
     */
    fun loadWalletAddress(username: String? = null, callback: (String) -> Unit) {
        launchUI(strategy) {
            if (managedMode) {
                val user = username ?: getUserName()
                dbInterface.findWallet(user) { wallet ->
                    var walletAddress = wallet?.walletAddress ?: ""
                    if (!bcInterface.isValidAddress(walletAddress)) {
                        walletAddress = ""
                    }
                    callback(walletAddress)
                }
            } else { // Unmanaged wallet mode
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
                        override fun onFailure(call: Call, e: IOException) {
                            Timber.d("ERROR: request call failed!")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            var jsonObject = JSONObject(response.body()?.string())
                            var walletAddress = ""
                            if (jsonObject.isNull("error")) {

                                if (!jsonObject.isNull("user")) {
                                    jsonObject = jsonObject.getJSONObject("user")

                                    if (!jsonObject.isNull("customFields")) {
                                        jsonObject = jsonObject.getJSONObject("customFields")
                                        walletAddress = jsonObject.getString("walletAddress")
                                        if (!bcInterface.isValidAddress(walletAddress)) {
                                            walletAddress = ""
                                        }
                                    }
                                }
                            } else {
                                Timber.d("ERROR: %s", jsonObject.getString("error"))
                            }
                            launchUI(strategy) {
                                callback(walletAddress)
                            }
                        }
                    })
                } catch (ex: Exception) {
                    Timber.e(ex)
                }
            }
        }
    }

    /**
     * Change the walletAddress field in the current user's customFields field
     *
     * @param address user's new wallet address
     *
     * NOTE: this function directly calls the REST API, which normally should be
     *          done in the Kotlin SDK
     */
    private fun updateWalletAddress(address: String) {
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
                    override fun onResponse(call: Call, response: Response) {}
                })
            } catch (ex: Exception) {
                Timber.e(ex)
                view.showManagedWalletNotSyncedWithRCProfile(ex.message)
            }
        }
    }

    fun getUserName(): String {
        return localRepository.get(LocalRepository.CURRENT_USERNAME_KEY) ?: ""
    }

    /**
     * Get all room names the user has open that are Direct Message rooms
     */
    fun loadDMRooms() {
        launchUI(strategy) {
            try {
                val roomList = getChatRoomsInteractor.getByName(serverUrl, "")
                val directMessageRoomList = roomList.filter( {it.type.javaClass == RoomType.DIRECT_MESSAGE.javaClass} )
                view.setupSendToDialog(directMessageRoomList.map({room -> room.name}))
            } catch (ex: RocketChatException) {
                Timber.e(ex)
            }
        }
    }

    /**
     * Find an open direct message room that matches a given username
     *  and redirect the user to the ChatRoom Activity, then immediately to a Transaction Activity
     */
    fun loadDMRoomByName(name: String) {
        launchUI(strategy) {
            try {
                val roomList = getChatRoomsInteractor.getByName(serverUrl, name)
                val directMessageRoomList = roomList.filter {it.type.javaClass == RoomType.DIRECT_MESSAGE.javaClass}

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

    fun loadSendToDialogUI(dialogView: View) {
        if (managedMode) {
            view.hideComplexSendToOptions(dialogView)
        }
    }

    fun getSendToDialogTitle(): String {
        return if (managedMode) "Search Users" else "Find Recipient"
    }
}