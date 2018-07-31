package chat.rocket.android.wallet.transaction.presentation

import android.content.Context
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import chat.rocket.android.server.domain.SettingsRepository
import chat.rocket.android.server.domain.TokenRepository
import chat.rocket.android.server.domain.isWalletManaged
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import chat.rocket.android.util.extensions.launchUI
import chat.rocket.android.util.retryIO
import chat.rocket.android.wallet.BlockchainInterface
import chat.rocket.android.wallet.WalletDBInterface
import chat.rocket.common.model.Token
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.rest.me
import kotlinx.coroutines.experimental.async
import okhttp3.*
import org.json.JSONObject
import org.spongycastle.asn1.x500.style.RFC4519Style.l
import org.spongycastle.crypto.tls.ConnectionEnd.client
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class TransactionPresenter @Inject constructor (private val view: TransactionView,
                                                private val strategy: CancelStrategy,
                                                private val tokenRepository: TokenRepository,
                                                private val localRepository: LocalRepository,
                                                serverInteractor: GetCurrentServerInteractor,
                                                settingsRepository: SettingsRepository,
                                                factory: RocketChatClientFactory) {

    private val serverUrl = serverInteractor.get()!!
    private val client: RocketChatClient = factory.create(serverUrl)
    private val restUrl: HttpUrl? = HttpUrl.parse(serverUrl)
    private val bcInterface = BlockchainInterface()
    private val dbInterface = WalletDBInterface()
    private val settings = settingsRepository.get(serverUrl)
    private val managedMode = settings.isWalletManaged()

    /**
     * Send a transaction on the blockchain
     *
     * @param password the sender's password to unlock his/her private key file
     * @param senderAddr wallet address of the sender
     * @param recipientAddr wallet address of the recipient
     * @param amount Double amount of ether being sent
     * @param c Context/Activity
     */
    fun sendTransaction(password: String, senderAddr: String, recipientAddr: String, amount: Double, c: Context, reason: String) {
        launchUI(strategy) {
            view.showLoading()
            async {
                try {
                    val txHash = bcInterface.sendTransaction(password, senderAddr, recipientAddr, amount, c)

                    dbInterface.updateTransactions(senderAddr, recipientAddr, txHash)

                    view.showSuccessfulTransaction(amount, txHash, reason)
                } catch (ex: Exception) {
                    view.hideLoading()
                    view.showTransactionFailedMessage(ex.message)
                    Timber.e(ex)
                }
            }
        }
    }

    /**
     * Fetch the current user's wallet balance
     */
    fun loadUserTokens() {
        launchUI(strategy) {
            view.showLoading()
            try {
                val me = retryIO("me") { client.me() }
                loadWalletAddress(me.username, {
                    if (it.isEmpty()) {
                        view.showNoWalletError()
                    } else {
                        view.showUserWallet(it, bcInterface.getBalance(it))
                    }
                    view.hideLoading()
                })
            } catch (ex: Exception) {
                view.hideLoading()
                Timber.e(ex)
            }
        }
    }

    /**
     * Managed mode: Retrieve the walletAddress field in a user's customFields object
     *
     * Unmanaged mode: Retrieve the user's walletAddress from the database
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
            view.showLoading()
            if (managedMode) {
                val user = username ?: getUserName()
                dbInterface.findWallet(user) { wallet ->
                    var walletAddress = wallet?.walletAddress ?: ""
                    if (!bcInterface.isValidAddress(walletAddress)) {
                        walletAddress = ""
                    }
                    view.hideLoading()
                    callback(walletAddress)
                }
            } else {
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
                            launchUI(strategy) {
                                view.hideLoading()
                                callback("")
                            }
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
                                view.hideLoading()
                                callback(walletAddress)
                            }
                        }
                    })
                } catch (ex: Exception) {
                    view.hideLoading()
                    Timber.e(ex)
                }
            }
        }
    }

    fun loadUI() {
        if (managedMode) {
            view.hideUnmanagedUI()
        }
    }

    fun getUserName(): String {
        return localRepository.get(LocalRepository.CURRENT_USERNAME_KEY) ?: ""
    }
}