package chat.rocket.android.wallet.ui

import java.math.BigDecimal
import java.util.*

class TransactionViewModel(hash: String, amount: BigDecimal, time: Long, sentFromUser: Boolean) {
    val txHash: String = hash
    val value: String = amount.toString()
    val timestamp: String = Date(time*1000).toString()
    // Whether this transaction was received from (true) or sent to (false) the current user
    val outgoingTx: Boolean = sentFromUser
}