package chat.rocket.android.wallet.transaction.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.support.v7.view.ActionMode
import android.view.*
import android.widget.Toast
import chat.rocket.android.R
import chat.rocket.android.R.id.*
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.showToast
import chat.rocket.android.wallet.WalletDBInterface
import chat.rocket.android.wallet.transaction.presentation.TransactionView
import dagger.android.support.AndroidSupportInjection
import chat.rocket.android.util.extensions.ui
import chat.rocket.android.wallet.transaction.presentation.TransactionPresenter
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_transaction.*


class TransactionFragment: Fragment(), TransactionView, android.support.v7.view.ActionMode.Callback {
    @Inject lateinit var presenter: TransactionPresenter
    private var actionMode: ActionMode? = null

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
        val nullableRecipientId = activity?.intent?.getStringExtra("recipient_user_name")
        val recipientId = nullableRecipientId ?: ""
        recipient.text = recipient.text.toString().plus(recipientId)

        button_transaction_send.setOnClickListener {

            // get token amount
            val amount = amount_tokens.text.toString().toDouble()

            // get userId of sender
            val nullableSenderId = activity?.intent?.getStringExtra("sender_user_name")
            val senderId = nullableSenderId ?: ""

            // update balances
            dbInterface?.sendTokens(senderId, recipientId, amount, { bal ->
                //textView_balance.textContent = bal.toString()
                showToast("Sent $amount tokens to $recipientId", Toast.LENGTH_LONG)
            })

            //go back to chat
            activity?.onBackPressed()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadUserTokens()
    }

    override fun showWalletBalance(balance: Double) {
        //button_transaction_send.textContent = balance.toString()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
//        return when (item.itemId) { // TODO
//            R.id.button_send_transaction -> {
//                presenter.sendTransaction()
//                mode.finish()
//                return true
//            }
//            else -> {
//                false
//            }
//        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        // TODO
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
    }

    private fun showToast(msg: String?) {
        ui {
            Toast.makeText(it, msg, Toast.LENGTH_LONG).show()
        }
    }

}