package chat.rocket.android.wallet.create.presentation

import android.util.Log
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.util.extensions.launchUI
import chat.rocket.android.wallet.BlockchainInterface
import chat.rocket.common.RocketChatException
import timber.log.Timber
import javax.inject.Inject

class CreateWalletPresenter @Inject constructor (private val view: CreateWalletView, private val strategy: CancelStrategy){

    private val bcInterface = BlockchainInterface(view.returnContext())

    fun createNewWallet(walletName: String, password: String){
        launchUI(strategy) {
            try {

                var mnemonic =  bcInterface.createBip39Wallet(password)
                view.showWalletSuccessfullyCreatedMessage(mnemonic)

            } catch (exception: Exception ) {
                view.showWalletCreationFailedMessage(exception.message)
            }
        }
    }
}