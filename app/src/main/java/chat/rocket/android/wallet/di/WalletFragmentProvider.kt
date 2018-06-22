package chat.rocket.android.wallet.di

import chat.rocket.android.wallet.ui.WalletFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class WalletFragmentProvider {
    @ContributesAndroidInjector(modules = [WalletFragmentModule::class])
    abstract fun provideWalletFragment(): WalletFragment
}