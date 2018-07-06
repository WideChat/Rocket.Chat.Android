package chat.rocket.android.wallet.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import chat.rocket.android.R
import chat.rocket.android.R.id.*
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.util.extensions.*
import chat.rocket.android.wallet.BlockchainInterface
import chat.rocket.android.wallet.create.ui.CreateWalletActivity
import chat.rocket.android.wallet.presentation.WalletPresenter
import chat.rocket.android.wallet.presentation.WalletView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.app_bar.*
import kotlinx.android.synthetic.main.emoji_keyboard.*
import kotlinx.android.synthetic.main.fragment_token_send.view.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.mnemonic_dialog.view.*
import timber.log.Timber
import javax.inject.Inject

class WalletFragment : Fragment(), WalletView {
    @Inject lateinit var presenter: WalletPresenter

    private var bcInterface: BlockchainInterface? = null
    private val NEW_WALLET_REQUEST = 1
    private val RESULT_OK = -1


    companion object {
        fun newInstance() = WalletFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bcInterface = BlockchainInterface(this.context)
        setupToolbar()

        // Check if user has a wallet
        var wallets = bcInterface?.findWallets()
        if( /*wallets!!.isNotEmpty()*/ wallets!!.size > 40){ //TODO

            showWallet(true)
        }
    }

    private fun setupToolbar() {
        (activity as MainActivity).toolbar.title = getString(R.string.title_wallet)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = container?.inflate(R.layout.fragment_wallet)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        button_create_wallet.setOnClickListener {
            ui {
                val intent = Intent(activity, CreateWalletActivity::class.java)
                intent.putExtra("user_name", presenter.getUserName())
                startActivityForResult(intent, NEW_WALLET_REQUEST)
                activity?.overridePendingTransition(R.anim.open_enter, R.anim.open_exit)
            }
        }

        button_delete_wallet.setOnClickListener {
//            dbInterface?.deleteWallet(presenter.getUserName(), {
//                showToast("Wallet Deleted!", Toast.LENGTH_LONG)
//                showWallet(false)
//            }) TODO remove... can't actually delete a wallet
        }

        button_buy.setOnClickListener {
            showBalance()
            // The passing of "e" is temporary and should be replaced by the recipient's username/id
            //  The name "e" returns the first DM room in the user's list of chatrooms that has an "e" in it
            presenter.loadDMRoomByName("e") //TODO move this to button_sendToken with search functionality
        }

        // Clicking send from wallet fragment shows "send" dialog
        button_sendToken.setOnClickListener {

            // TODO remove this dialog and instead redirect to a TransactionActivity to handle the transaction
            // Inflate with custom view
            val sendDialogView = LayoutInflater.from(activity).inflate(R.layout.fragment_token_send, null)
            val sendDialogBuilder = AlertDialog.Builder(activity)
                    .setView(sendDialogView)
                    .setTitle("Send Tokens")

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
                    val senderId = presenter.getUserName()

                    // get userId of recipient
                    val recipientId = sendDialogView.recipient.text.toString()

                    // update balances TODO
//                    dbInterface?.sendTokens(senderId, recipientId, amount, {bal ->
//                        textView_balance.textContent = bal.toString()
//                        showToast("Sent $amount tokens to $recipientId", Toast.LENGTH_LONG)
//                    })
                    sendAlertDialog.dismiss()
                }
            }

            // on click of "Cancel" close the window
            sendDialogView.button_cancel.setOnClickListener{
                sendAlertDialog.dismiss()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == NEW_WALLET_REQUEST){
            if(resultCode == RESULT_OK){

                val mnemonicDialogView = LayoutInflater.from(activity).inflate(R.layout.mnemonic_dialog, null)
                val mnemonicDialogBuilder = AlertDialog.Builder(activity)
                        .setView(mnemonicDialogView)

                // display phrase
                var mnemonic = data!!.getStringExtra("mnemonic")
                mnemonicDialogView.mnemonic.textContent = mnemonic

                // show dialog
                val mnemonicAlertDialog = mnemonicDialogBuilder.create()

                // set listener
                mnemonicDialogView.button_mnemonic_saved.setOnClickListener{
                    mnemonicAlertDialog.dismiss()
                }

                mnemonicAlertDialog.show()
            }
        }
    }

    override fun showRoomFailedToLoadMessage(name: String) {
        showToast("No direct message chat room open with user: $name", Toast.LENGTH_LONG)
    }

    override fun showBalance() {
        // currently, only the balance of the first wallet in the list is displayed... TODO eventually allow user to have/access multiple wallets
        var wallets = bcInterface?.findWallets()
        var bal = bcInterface?.getBalance(wallets!![0])
        textView_balance.textContent = bal.toString()
    }

    override fun showWallet(value: Boolean) {
        button_create_wallet.setVisible(!value)
        button_buy.setVisible(value)
        button_sendToken.setVisible(value)
        textView_transactions.setVisible(value)
        textView_balance.setVisible(value)
        textView_wallet_title.setVisible(value)
        divider_wallet.setVisible(value)
        //button_delete_wallet.setVisible(value)
        if (value)
            showBalance()
    }

}
