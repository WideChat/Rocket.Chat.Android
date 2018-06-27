package chat.rocket.android.wallet.create.presentation

import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import chat.rocket.core.RocketChatClient
import javax.inject.Inject

class CreateWalletPresenter @Inject constructor (private val view: CreateWalletView, private val strategy: CancelStrategy){

    fun createNewWallet(walletName: String, password: String){

    }
}