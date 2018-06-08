package chat.rocket.android.wallet.transaction.presentation

interface TransactionView {

    fun showWalletBalance(balance: Double)

    fun showTransactionSuccess(recipient: String, amount: Double)
}