package chat.rocket.android.wallet.ui

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import chat.rocket.android.R
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.textContent
import chat.rocket.android.wallet.WalletDBInterface
import chat.rocket.android.wallet.presentation.WalletView
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.fragment_token_send.*
import kotlinx.android.synthetic.main.fragment_token_send.view.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.nav_header.*


class WalletFragment : Fragment(), WalletView {
    private var dbInterface: WalletDBInterface? = null

    companion object {
        fun newInstance() = WalletFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbInterface = WalletDBInterface()
        setupToolbar()
        showBalance()
    }

    private fun setupToolbar() {
        (activity as MainActivity).toolbar.title = getString(R.string.title_wallet)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = container?.inflate(R.layout.fragment_wallet)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        button_buy.setOnClickListener {
            showBalance()
            //nothing for now TODO
        }

        // Clicking send from wallet fragment shows "send" dialog
        button_sendToken.setOnClickListener {

            // Inflate with custom view
            val sendDialogView = LayoutInflater.from(activity).inflate(R.layout.fragment_token_send, null)
            val sendDialogBuilder = AlertDialog.Builder(activity)
                    .setView(sendDialogView)
                    .setTitle("SendTokens")

            // show dialog
            val sendAlertDialog = sendDialogBuilder.show()

            // on click of "Confirm"
            sendDialogView.button_confirm.setOnClickListener{

                //TODO null/empty checks for input fields

                // get token amount
                val amount = sendDialogView.amount.text.toString().toDouble()

                // get userId of sender
                val nullableSenderId = activity?.text_user_name?.textContent
                val senderId = nullableSenderId ?: ""

                // get userId of recipient
                val recipientId = sendDialogView.recipient.text.toString()

                // update balances
                dbInterface?.sendTokens(senderId, recipientId, amount, {bal -> textView_balance.textContent = bal.toString()})

                sendAlertDialog.dismiss()
            }

            // on click of "Cancel" close the window
            sendDialogView.button_cancel.setOnClickListener{
                sendAlertDialog.dismiss()
            }
        }
    }

    private fun showBalance() {
        dbInterface?.getBalance(activity?.text_user_name?.textContent, {bal -> textView_balance.textContent = bal.toString()})
    }

}
