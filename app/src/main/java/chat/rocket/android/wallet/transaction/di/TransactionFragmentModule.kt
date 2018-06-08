package chat.rocket.android.wallet.transaction.di

import android.arch.lifecycle.LifecycleOwner
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.dagger.scope.PerFragment
import chat.rocket.android.wallet.transaction.presentation.TransactionView
import chat.rocket.android.wallet.transaction.ui.TransactionFragment
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.experimental.Job

@Module
@PerFragment
class TransactionFragmentModule {
    @Provides
    fun transactionView(frag: TransactionFragment): TransactionView {
        return frag
    }

    @Provides
    fun settingsLifecycleOwner(frag: TransactionFragment): LifecycleOwner {
        return frag
    }

    @Provides
    fun provideCancelStrategy(owner: LifecycleOwner, jobs: Job): CancelStrategy {
        return CancelStrategy(owner, jobs)
    }
}