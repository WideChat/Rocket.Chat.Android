package chat.rocket.android.userdetails.presentation

import chat.rocket.android.chatroom.presentation.ChatRoomNavigator
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.db.DatabaseManager
import chat.rocket.android.server.domain.CurrentServerRepository
import chat.rocket.android.server.domain.GetSettingsInteractor
import chat.rocket.android.server.domain.PermissionsInteractor
import chat.rocket.android.server.domain.TokenRepository
import chat.rocket.android.server.infraestructure.ConnectionManagerFactory
import org.junit.Before
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import testConfig.Config.Companion.CURRENT_SERVER


class UserDetailsPresenterTest {

    val view = mock(UserDetailsView::class.java)
    val dbManager = mock(DatabaseManager::class.java)
    val strategy = mock(CancelStrategy::class.java)
    val navigator = mock(ChatRoomNavigator::class.java)
    val permissionsInteractor = mock(PermissionsInteractor::class.java)
    val tokenRepository = mock(TokenRepository::class.java)
    val settingsInteractor = mock(GetSettingsInteractor::class.java)
    val serverInteractor = mock(CurrentServerRepository::class.java)
    val factory = mock(ConnectionManagerFactory::class.java)

    lateinit var userDetailsPresenter: UserDetailsPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(serverInteractor.get()).thenReturn(CURRENT_SERVER)
        userDetailsPresenter = UserDetailsPresenter(
            view, dbManager, strategy, navigator, permissionsInteractor,
            tokenRepository, settingsInteractor, serverInteractor, factory
        )
    }
}