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
import chat.rocket.android.util.extensions.asObservable
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.textContent
import chat.rocket.android.wallet.transaction.presentation.TransactionView
import dagger.android.support.AndroidSupportInjection
import chat.rocket.android.util.extensions.ui
import chat.rocket.android.wallet.transaction.presentation.TransactionPresenter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_transaction.*


class TransactionFragment: Fragment(), TransactionView, android.support.v7.view.ActionMode.Callback {
    @Inject lateinit var presenter: TransactionPresenter
    private var actionMode: ActionMode? = null
    private var recipientId: String = ""
    private val disposables = CompositeDisposable()


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

        // get recipient ID
        val nullableRecipientId = activity?.intent?.getStringExtra("recipient_user_name")
        recipientId = nullableRecipientId ?: ""
        text_recipient.text = text_recipient.text.toString().plus(recipientId)

        button_transaction_send.setOnClickListener {

            // get token amount
            val amount = amount_tokens.text.toString().toDouble()

            presenter.sendTransaction(recipientId, amount)
            //go back to chat
            activity?.onBackPressed()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadUserTokens()

        disposables.add(listenToChanges())
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    override fun showWalletBalance(balance: Double) {
        text_current_balance.textContent = "Current Balance: " + balance.toString()
    }

    override fun showTransactionSuccess(recipient: String, amount: Double) {
        showToast("Sent $amount tokens to $recipient")
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_password -> {
                val amount = amount_tokens.text.toString().toDouble()

                presenter.sendTransaction(recipientId, amount)

                mode.finish()
                //go back to chat
                activity?.onBackPressed()
                return true
            }
            else -> {
                false
            }
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        mode.menuInflater.inflate(R.menu.password, menu)
        mode.title = getString(R.string.action_confirm_transaction)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
    }

    private fun listenToChanges(): Disposable {
        return amount_tokens.asObservable().subscribe {
            val amountText = amount_tokens.textContent

            if (amountText.isNotEmpty() && amountText != "." && amountText.toDouble() > 0.0)
                startActionMode()
            else
                finishActionMode()
        }
    }

    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (activity as TransactionActivity).startSupportActionMode(this)
        }
    }

    private fun finishActionMode() = actionMode?.finish()

    private fun showToast(msg: String?) {
        ui {
            Toast.makeText(it, msg, Toast.LENGTH_LONG).show()
        }
    }

}