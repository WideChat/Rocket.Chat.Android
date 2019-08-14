package chat.rocket.android.authentication.registerusername.presentation

import chat.rocket.android.analytics.AnalyticsManager
import chat.rocket.android.authentication.presentation.AuthenticationNavigator
import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.server.domain.*
import chat.rocket.android.server.domain.model.Account
import chat.rocket.android.server.infraestructure.RocketChatClientFactory
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import testConfig.Config
import testConfig.Config.Companion.CURRENT_SERVER
import testConfig.Config.Companion.UPDATED_AVATAR
import testConfig.Config.Companion.USERNAME
import testConfig.Config.Companion.USER_NAME

class RegisterUsernamePresenterTest {

    private val view = mock(RegisterUsernameView::class.java)
    private val strategy = mock(CancelStrategy::class.java)
    private val navigator = mock(AuthenticationNavigator::class.java)
    private val tokenRepository = mock(TokenRepository::class.java)
    private val settingsInteractor = mock(GetSettingsInteractor::class.java)
    private val analyticsManager = mock(AnalyticsManager::class.java)
    private val saveCurrentServer = mock(SaveCurrentServerInteractor::class.java)
    private val saveAccountInteractor = mock(SaveAccountInteractor::class.java)
    private val factory = mock(RocketChatClientFactory::class.java)
    private val serverInteractor = mock(GetConnectingServerInteractor::class.java)

    private lateinit var registerUsernamePresenter: RegisterUsernamePresenter

    private val account = Account(
        CURRENT_SERVER, null, null,
        USERNAME, UPDATED_AVATAR
    )

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(serverInteractor.get()).thenReturn(CURRENT_SERVER)
        registerUsernamePresenter = RegisterUsernamePresenter(
            view, strategy, navigator, tokenRepository, saveAccountInteractor, analyticsManager,
            saveCurrentServer, serverInteractor, factory, settingsInteractor
        )
    }

    @Test
    fun `check account is saved`() {
        val method = registerUsernamePresenter.javaClass.getDeclaredMethod("saveAccount", String::class.java)
        method.isAccessible = true
        val parameters = arrayOfNulls<Any>(1)
        parameters[0] = Config.USERNAME
        method.invoke(registerUsernamePresenter, *parameters)
        verify(saveAccountInteractor).save(account)
    }
}