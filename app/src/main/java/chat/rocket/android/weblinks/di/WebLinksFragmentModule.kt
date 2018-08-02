package chat.rocket.android.weblinks.di

import androidx.lifecycle.LifecycleOwner
import chat.rocket.android.dagger.scope.PerFragment
import chat.rocket.android.weblinks.presentation.WebLinksView
import chat.rocket.android.weblinks.ui.WebLinksFragment
import dagger.Module
import dagger.Provides

@Module
class WebLinksFragmentModule {

    @Provides
    fun webLinksView(frag: WebLinksFragment): WebLinksView {
        return frag
    }

    @Provides
    @PerFragment
    fun provideLifecycleOwner(frag: WebLinksFragment): LifecycleOwner {
        return frag
    }
}