package chat.rocket.android.wallet.transaction.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import chat.rocket.android.R
import chat.rocket.android.R.id.button_transaction_send
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.showToast
import chat.rocket.android.util.extensions.textContent
import chat.rocket.android.wallet.WalletDBInterface
import chat.rocket.android.wallet.transaction.presentation.TransactionView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_token_send.view.*
import kotlinx.android.synthetic.main.fragment_transaction.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.nav_header.*


class TransactionFragment: Fragment(), TransactionView {

    private var dbInterface: WalletDBInterface? = null

    companion object {
        fun newInstance() = TransactionFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = container?.inflate(R.layout.fragment_transaction)

    override fun onActivityCreated(savedInstanceState: Bundle?){
        super.onActivityCreated(savedInstanceState)

        dbInterface = WalletDBInterface()

        // get recipient ID
        val nullableRecipientId = activity?.intent?.getStringExtra("user_name") //recipient_user_name
        val recipientId = nullableRecipientId ?: ""
        recipient.text = recipient.text.toString().plus(recipientId)

        button_transaction_send.setOnClickListener{

            // get token amount
            val amount = amount_tokens.text.toString().toDouble()

            // get userId of sender
            val nullableSenderId = activity?.intent?.getStringExtra("sender_user_name")
            val senderId = nullableSenderId ?: ""

            // update balances
            dbInterface?.sendTokens(senderId, recipientId, amount, {bal ->
                //textView_balance.textContent = bal.toString()
                showToast("Sent $amount tokens to $recipientId", Toast.LENGTH_LONG)
            })

            //go back to chat
            activity?.onBackPressed()
        }
    }

}