package chat.rocket.android.wallet.ui

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import chat.rocket.android.R
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.setVisible
import chat.rocket.android.util.extensions.showToast
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
        // Check if user has a wallet (in the database)
        dbInterface?.findWallet(activity?.text_user_name?.textContent, {wallet ->
            if (wallet != null) {
                // Show this user's existing wallet
                showWallet()
            }
        })
    }

    private fun setupToolbar() {
        (activity as MainActivity).toolbar.title = getString(R.string.title_wallet)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = container?.inflate(R.layout.fragment_wallet)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        button_create_wallet.setOnClickListener {
            dbInterface?.createWallet(activity?.text_user_name?.textContent, {
                showToast("Wallet Created!", Toast.LENGTH_LONG)
                showWallet()
            })
        }

        button_delete_wallet.setOnClickListener {
            dbInterface?.deleteWallet(activity?.text_user_name?.textContent, {
                showToast("Wallet Deleted!", Toast.LENGTH_LONG)
                hideWallet()
            })
        }

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

                // empty checks for input fields
                if (sendDialogView.amount.text.toString() == "" || sendDialogView.recipient.text.toString() == "") {
                    sendAlertDialog.dismiss()
                    showToast("Transaction failed!", Toast.LENGTH_LONG)
                }
                else {
                    // get token amount
                    val amount = sendDialogView.amount.text.toString().toDouble()

                    // get userId of sender
                    val nullableSenderId = activity?.text_user_name?.textContent
                    val senderId = nullableSenderId ?: ""

                    // get userId of recipient
                    val recipientId = sendDialogView.recipient.text.toString()

                    // update balances
                    dbInterface?.sendTokens(senderId, recipientId, amount, {bal ->
                        textView_balance.textContent = bal.toString()
                        showToast("Sent $amount tokens to $recipientId", Toast.LENGTH_LONG)
                    })
                    sendAlertDialog.dismiss()
                }
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

    private fun showBalance(bal: Double) {
        textView_balance.textContent = bal.toString()
    }

    private fun showWallet() {
        button_create_wallet.setVisible(false)
        button_buy.setVisible(true)
        button_sendToken.setVisible(true)
        textView_transactions.setVisible(true)
        textView_balance.setVisible(true)
        textView_wallet_title.setVisible(true)
        divider_wallet.setVisible(true)
        button_delete_wallet.setVisible(true)
        showBalance()
    }

    private fun hideWallet() {
        button_create_wallet.setVisible(true)
        button_buy.setVisible(false)
        button_sendToken.setVisible(false)
        textView_transactions.setVisible(false)
        textView_balance.setVisible(false)
        textView_wallet_title.setVisible(false)
        divider_wallet.setVisible(false)
        button_delete_wallet.setVisible(false)
    }

}
