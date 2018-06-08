package chat.rocket.android.wallet.transaction.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import chat.rocket.android.R
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.wallet.transaction.presentation.TransactionView
import dagger.android.support.AndroidSupportInjection


class TransactionFragment: Fragment(), TransactionView {

    companion object {
        fun newInstance() = TransactionFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = container?.inflate(R.layout.fragment_transaction)

}