package chat.rocket.android.wallet.create.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.view.ActionMode
import android.view.*
import android.widget.Toast
import chat.rocket.android.R
import chat.rocket.android.util.extensions.*
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.wallet.create.presentation.CreateWalletPresenter
import chat.rocket.android.wallet.create.presentation.CreateWalletView
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_create_wallet.*

class CreateWalletFragment:  Fragment(), CreateWalletView, android.support.v7.view.ActionMode.Callback {
    @Inject lateinit var presenter: CreateWalletPresenter
    private var actionMode: ActionMode? = null
    private val disposables = CompositeDisposable()

    companion object {
        fun newInstance() = CreateWalletFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = container?.inflate(R.layout.fragment_create_wallet)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        disposables.add(listenToChanges())
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_password -> {

                mode.finish()

                val walletName = textView_name_wallet.text.toString()
                val password = textView_create_password.text.toString()
                presenter.createNewWallet(walletName, password)

                (activity as CreateWalletActivity).setupResultAndFinish(walletName, password)

                return true
            }
            else -> {
                false
            }
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        mode.menuInflater.inflate(R.menu.password, menu)
        mode.title = getString(R.string.action_confirm_create_wallet)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
    }


    override fun showWalletSuccessfullyCreatedMessage() {
        showToast("Wallet was successfully created")

    }

    override fun showWalletCreationFailedMessage(error : String?) {
        showToast("Failed to create wallet: " + error)
    }

    override fun returnContext(): Context?{
        return this.context
    }

    private fun finishActionMode() = actionMode?.finish()

    private fun listenToChanges(): Disposable {
        return editText_confirm_password.asObservable().subscribe {
            val passText = editText_password.textContent
            val confirmPass = editText_confirm_password.textContent

            if (passText.isNotEmpty() && passText != "" && confirmPass.isNotEmpty() && confirmPass == passText)
                startActionMode()
            else
                finishActionMode()
        }
    }

    private fun showToast(msg: String?) {
        ui {
            Toast.makeText(it, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (activity as CreateWalletActivity).startSupportActionMode(this)
        }
    }

}