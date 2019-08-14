package chat.rocket.android.main.presentation

import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.db.DatabaseManagerFactory
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.main.uimodel.NavHeaderUiModelMapper
import chat.rocket.android.push.GroupedPush
import chat.rocket.android.server.domain.*
import chat.rocket.android.server.infraestructure.ConnectionManagerFactory
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import testConfig.Config.Companion.ADMIN_PANEL_URL
import testConfig.Config.Companion.CHAT_ROOM_ID
import testConfig.Config.Companion.CURRENT_SERVER


class MainPresenterTest {

    private val view = Mockito.mock(MainView::class.java)
    private val strategy = Mockito.mock(CancelStrategy::class.java)
    private val navigator = Mockito.mock(MainNavigator::class.java)
    private val tokenRepository = Mockito.mock(TokenRepository::class.java)
    private val refreshSettingsInteractor = Mockito.mock(RefreshSettingsInteractor::class.java)
    private val refreshPermissionsInteractor = Mockito.mock(RefreshPermissionsInteractor::class.java)
    private val navHeaderMapper = Mockito.mock(NavHeaderUiModelMapper::class.java)
    private val saveAccountInteractor = Mockito.mock(SaveAccountInteractor::class.java)
    private val getAccountsInteractor = Mockito.mock(GetAccountsInteractor::class.java)
    private val groupedPush = Mockito.mock(GroupedPush::class.java)
    private val serverInteractor = Mockito.mock(GetCurrentServerInteractor::class.java)
    private val localRepository = Mockito.mock(LocalRepository::class.java)
    private val removeAccountInteractor = Mockito.mock(RemoveAccountInteractor::class.java)
    private val factory = Mockito.mock(RocketChatClientFactory::class.java)
    private val dbManagerFactory = Mockito.mock(DatabaseManagerFactory::class.java)
    private val getSettingsInteractor = Mockito.mock(GetSettingsInteractor::class.java)
    private val managerFactory = Mockito.mock(ConnectionManagerFactory::class.java)

    lateinit var mainPresenter: MainPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(serverInteractor.get()).thenReturn(CURRENT_SERVER)
        mainPresenter = MainPresenter(
            view, strategy, navigator, tokenRepository, refreshSettingsInteractor, refreshPermissionsInteractor,
            navHeaderMapper, saveAccountInteractor, getAccountsInteractor, groupedPush, serverInteractor,
            localRepository, removeAccountInteractor, factory, dbManagerFactory, getSettingsInteractor, managerFactory
        )
    }

    @Test
    fun `navigate to chatlist`(){
        mainPresenter.toChatList(CHAT_ROOM_ID, null)
        verify(navigator).toChatList(CHAT_ROOM_ID, null)
    }

    @Test
    fun navigateToUserProfile() {
        mainPresenter.toUserProfile()
        verify(navigator).toUserProfile()
    }

    @Test
    fun navigateToAdmin() {
        mainPresenter.toAdminPanel()
        val a = tokenRepository.get(CURRENT_SERVER)
        a?.authToken?.let { verify(navigator).toAdminPanel(ADMIN_PANEL_URL, it) }
    }

    @Test
    fun `navigate to setting`() {
        mainPresenter.toSettings()
        verify(navigator).toSettings()
    }

    @Test
    fun `navigate to create channel`() {
        mainPresenter.toCreateChannel()
        verify(navigator).toCreateChannel()
    }

    @Test
    fun `add new server`() {
        mainPresenter.addNewServer()
        verify(navigator).toServerScreen()
    }
}