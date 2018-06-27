package chat.rocket.android.wallet.create.di

import chat.rocket.android.wallet.create.ui.CreateWalletFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class CreateWalletFragmentProvider {
    @ContributesAndroidInjector(modules = [CreateWalletFragmentModule::class])
    abstract fun provideCreateWalletFragment(): CreateWalletFragment

}