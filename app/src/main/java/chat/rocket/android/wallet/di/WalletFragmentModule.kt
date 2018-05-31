package chat.rocket.android.wallet.di

import android.arch.lifecycle.LifecycleOwner
import chat.rocket.android.dagger.scope.PerFragment
import chat.rocket.android.wallet.presentation.WalletView
import chat.rocket.android.wallet.ui.WalletFragment
import dagger.Module
import dagger.Provides

@Module
@PerFragment
class WalletFragmentModule {

    @Provides
    fun walletView(frag: WalletFragment): WalletView {
        return frag
    }

    @Provides
    fun provideLifecycleOwner(frag: WalletFragment): LifecycleOwner {
        return frag
    }
}