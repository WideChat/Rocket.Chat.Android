package chat.rocket.android.wallet.presentation

import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.wallet.transaction.presentation.TransactionView
import javax.inject.Inject

class WalletPresenter @Inject constructor (private val localRepository: LocalRepository) {

    fun getUserName(): String {
        return localRepository.get(LocalRepository.CURRENT_USERNAME_KEY) ?: ""
    }

}