package chat.rocket.android.wallet.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import chat.rocket.android.R
import chat.rocket.android.main.ui.MainActivity

import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.wallet.presentation.WalletView
import kotlinx.android.synthetic.main.app_bar.*

class WalletFragment : Fragment(), WalletView {

    companion object {
        fun newInstance() = WalletFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
    }

    private fun setupToolbar() {
        (activity as MainActivity).toolbar.title = getString(R.string.title_wallet)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = container?.inflate(R.layout.fragment_wallet)
}
