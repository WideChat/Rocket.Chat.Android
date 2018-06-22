package chat.rocket.android.wallet.presentation

import chat.rocket.core.model.ChatRoom

interface WalletView {

    /**
     * Retrieve an updated wallet balance from the backend and display it
     */
    fun showBalance()

    /**
     * Switch between displaying the wallet UI (balance, send button, transaction history, etc)
     *  and displaying the UI for creating a new wallet
     */
    fun showWallet(value: Boolean = true)

    /**
     * When trying to send tokens through a direct message room that doesn't exist,
     *  keep the user in the WalletFragment and display a message
     *
     *  @param name The name of the chat room that was searched for
     */
    fun showRoomFailedToLoadMessage(name: String)

}
