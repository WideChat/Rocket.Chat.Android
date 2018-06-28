package chat.rocket.android.wallet.create.presentation

import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.util.extensions.launchUI
import chat.rocket.common.RocketChatException
import javax.inject.Inject

class CreateWalletPresenter @Inject constructor (private val view: CreateWalletView, private val strategy: CancelStrategy){

    fun createNewWallet(walletName: String, password: String){
        launchUI(strategy) {
            try {
                //TODO connect blockchain backend
                view.showWalletSuccessfullyCreatedMessage()
            } catch (exception: RocketChatException) {
                view.showWalletCreationFailedMessage(exception.message)
            }
        }
    }
}