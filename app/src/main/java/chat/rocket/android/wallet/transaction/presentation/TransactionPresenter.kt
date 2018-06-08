package chat.rocket.android.wallet.transaction.presentation

import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.wallet.WalletDBInterface
import javax.inject.Inject

class TransactionPresenter @Inject constructor (private val view: TransactionView,
                                                private val strategy: CancelStrategy,
                                                private val localRepository: LocalRepository) {

    private val dbInterface = WalletDBInterface()

    fun sendTransaction() {
        //TODO
    }

    fun loadUserTokens() {
        val userId = localRepository.get(LocalRepository.CURRENT_USERNAME_KEY) ?: ""
        dbInterface.getBalance(userId, {
            view.showWalletBalance(it)
        })
    }

}