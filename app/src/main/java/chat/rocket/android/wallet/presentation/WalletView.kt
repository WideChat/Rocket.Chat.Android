package chat.rocket.android.wallet.presentation

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

}
