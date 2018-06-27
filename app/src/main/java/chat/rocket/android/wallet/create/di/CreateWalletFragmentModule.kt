package chat.rocket.android.wallet.create.di

import android.arch.lifecycle.LifecycleOwner
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.dagger.scope.PerFragment
import chat.rocket.android.wallet.create.presentation.CreateWalletView
import chat.rocket.android.wallet.create.ui.CreateWalletFragment
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.Job

@Module
@PerFragment
class CreateWalletFragmentModule{
    @Provides
    fun createWalletView(frag: CreateWalletFragment): CreateWalletView{
        return frag
    }

    @Provides
    fun settingsLifecycleOwner(frag: CreateWalletFragment): LifecycleOwner {
        return frag
    }

    @Provides
    fun provideCancelStrategy(owner: LifecycleOwner, jobs: Job): CancelStrategy {
        return CancelStrategy(owner, jobs)
    }
}