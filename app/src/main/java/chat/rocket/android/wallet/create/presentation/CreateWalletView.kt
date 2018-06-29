package chat.rocket.android.wallet.create.presentation

import android.content.Context

interface CreateWalletView {

    fun showWalletSuccessfullyCreatedMessage()

    fun showWalletCreationFailedMessage(error : String?)

    fun returnContext():Context?

}