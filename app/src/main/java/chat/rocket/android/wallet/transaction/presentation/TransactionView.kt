package chat.rocket.android.wallet.transaction.presentation

import java.math.BigDecimal

interface TransactionView {

    fun showUserWallet(address: String, balance: BigDecimal)

    fun showRecipientAddress(address: String)

    fun showSuccessfulTransaction(amount: Double, txHash: String)

    fun showNoAddressError()

    fun showNoWalletError()

    fun showTransactionFailedMessage(msg: String?)

    fun showLoading()

    fun hideLoading()
}