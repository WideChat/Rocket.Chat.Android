package chat.rocket.android.chatrooms.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import chat.rocket.android.R
import chat.rocket.android.analytics.AnalyticsManager
import chat.rocket.android.analytics.event.ScreenViewEvent
import chat.rocket.android.chatrooms.adapter.RoomsAdapter
import chat.rocket.android.chatrooms.presentation.ChatRoomsPresenter
import chat.rocket.android.chatrooms.presentation.ChatRoomsView
import chat.rocket.android.chatrooms.viewmodel.ChatRoomsViewModel
import chat.rocket.android.chatrooms.viewmodel.ChatRoomsViewModelFactory
import chat.rocket.android.chatrooms.viewmodel.LoadingState
import chat.rocket.android.chatrooms.viewmodel.Query
import chat.rocket.android.db.DatabaseManager
import chat.rocket.android.helper.ChatRoomsSortOrder
import chat.rocket.android.helper.Constants
import chat.rocket.android.helper.SharedPreferenceHelper
import chat.rocket.android.util.extension.onQueryTextListener
import chat.rocket.android.util.extensions.fadeIn
import chat.rocket.android.util.extensions.fadeOut
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.showToast
import chat.rocket.android.util.extensions.ui
import chat.rocket.android.widget.DividerItemDecoration
import chat.rocket.core.internal.realtime.socket.model.State
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_chat_rooms.*
import timber.log.Timber
import javax.inject.Inject

// WIDECHAT
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.settings.ui.SettingsFragment
import chat.rocket.android.profile.ui.ProfileFragment
import android.widget.ImageButton
import kotlinx.android.synthetic.main.app_bar.*
//import android.graphics.Color
import chat.rocket.android.helper.UserHelper
//import chat.rocket.android.util.extensions.avatarUrl
import chat.rocket.android.server.domain.GetCurrentServerInteractor

internal const val TAG_CHAT_ROOMS_FRAGMENT = "ChatRoomsFragment"

private const val BUNDLE_CHAT_ROOM_ID = "BUNDLE_CHAT_ROOM_ID"

class ChatRoomsFragment : Fragment(), ChatRoomsView {
    @Inject
    lateinit var presenter: ChatRoomsPresenter
    @Inject
    lateinit var factory: ChatRoomsViewModelFactory
    @Inject
    lateinit var dbManager: DatabaseManager // TODO - remove when moving ChatRoom screen to DB
    @Inject
    lateinit var analyticsManager: AnalyticsManager
    lateinit var viewModel: ChatRoomsViewModel
    private var searchView: SearchView? = null
    private var sortView: MenuItem? = null
    private val handler = Handler()
    private var chatRoomId: String? = null
    private var progressDialog: ProgressDialog? = null
    // WIDECHAT
    private var settingsView: MenuItem? = null
    private var profileButton: ImageButton? = null
    // EAR >> these things for getting avatar??
    //private var serverInteractor: GetCurrentServerInteractor? = null
    //private var userHelper: UserHelper? = null
    //private var myselfName: userHelper.user()?.name ?: ""
    //private var avatarUrl: avatarUrl(myselfName)



    companion object {
        fun newInstance(chatRoomId: String? = null): ChatRoomsFragment {
            return ChatRoomsFragment().apply {
                arguments = Bundle(1).apply {
                    putString(BUNDLE_CHAT_ROOM_ID, chatRoomId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
        setHasOptionsMenu(true)
        val bundle = arguments
        if (bundle != null) {
            chatRoomId = bundle.getString(BUNDLE_CHAT_ROOM_ID)
            chatRoomId?.let {
                // TODO - bring back support to load a room from id.
                //presenter.goToChatRoomWithId(it)
                chatRoomId = null
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(dismissStatus)
        super.onDestroy()
    }

    override fun onResume() {
        // WIDECHAT - cleanup any titles set but other fragments
        if (Constants.WIDECHAT) {
            (activity as AppCompatActivity?)?.supportActionBar?.setDisplayShowTitleEnabled(false)
            searchView?.clearFocus()
        }
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_chat_rooms)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this, factory).get(ChatRoomsViewModel::class.java)
        subscribeUi()

        setupToolbar()

        analyticsManager.logScreenView(ScreenViewEvent.ChatRooms)
    }

    private fun subscribeUi() {
        ui {
            val adapter = RoomsAdapter { room ->
                presenter.loadChatRoom(room)
            }

            recycler_view.layoutManager = LinearLayoutManager(it)
            recycler_view.addItemDecoration(
                DividerItemDecoration(
                    it,
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_start),
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_end)
                )
            )
            recycler_view.itemAnimator = DefaultItemAnimator()
            recycler_view.adapter = adapter

            viewModel.getChatRooms().observe(viewLifecycleOwner, Observer { rooms ->
                rooms?.let {
                    Timber.d("Got items: $it")
                    adapter.values = it
                    if (rooms.isNotEmpty()) {
                        text_no_data_to_display.isVisible = false
                    }
                }
            })

            viewModel.loadingState.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is LoadingState.Loading -> if (state.count == 0L) showLoading()
                    is LoadingState.Loaded -> {
                        hideLoading()
                        if (state.count == 0L) showNoChatRoomsToDisplay()
                    }
                    is LoadingState.Error -> {
                        hideLoading()
                        showGenericErrorMessage()
                    }
                }
            })

            viewModel.getStatus().observe(viewLifecycleOwner, Observer { status ->
                status?.let { showConnectionState(status) }
            })

            updateSort()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        
        inflater.inflate(R.menu.chatrooms, menu)

        sortView = menu.findItem(R.id.action_sort)
        // WIDECHAT
        settingsView = menu.findItem(R.id.action_settings)

        if (Constants.WIDECHAT) {
            sortView?.isVisible = false
        } else {
            settingsView?.isVisible = false
        }

        val searchItem = menu.findItem(R.id.action_search)
        // WIDECHAT - this expands the action by default
        if (Constants.WIDECHAT) {
            searchItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
        } else {
            // WIDECHAT - using this will cover the settings icon
            searchView?.maxWidth = Integer.MAX_VALUE
        }

        searchView = searchItem?.actionView as? SearchView
        searchView?.setIconifiedByDefault(false)
        // EAR -test
        //searchView?.onActionViewExpanded()
        //searchView?.setIconified(false)
        //searchView?.setFocusable(false)
        //searchView?.isFocusableInTouchMode
        //searchView?.clearFocus()
        //searchView?.maxWidth = 1100
        //searchView?.setGravity(Gravity.CENTER_HORIZONTAL)
        //searchView?.setBackgroundColor(Color.WHITE)
        //searchView?.marginRight

        searchView?.onQueryTextListener { queryChatRoomsByName(it) }

        val expandListener = object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // Simply setting sortView to visible won't work, so we invalidate the options
                // to recreate the entire menu...
                activity?.invalidateOptionsMenu()
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                sortView?.isVisible = false
                return true
            }
        }
        searchItem?.setOnActionExpandListener(expandListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // TODO - simplify this
            R.id.action_sort -> {
                val dialogLayout = layoutInflater.inflate(R.layout.chatroom_sort_dialog, null)
                val sortType = SharedPreferenceHelper.getInt(
                    Constants.CHATROOM_SORT_TYPE_KEY,
                    ChatRoomsSortOrder.ACTIVITY
                )
                val groupByType =
                    SharedPreferenceHelper.getBoolean(Constants.CHATROOM_GROUP_BY_TYPE_KEY, false)

                val radioGroup = dialogLayout.findViewById<RadioGroup>(R.id.radio_group_sort)
                val groupByTypeCheckBox =
                    dialogLayout.findViewById<CheckBox>(R.id.checkbox_group_by_type)

                radioGroup.check(
                    when (sortType) {
                        0 -> R.id.radio_sort_alphabetical
                        else -> R.id.radio_sort_activity
                    }
                )
                radioGroup.setOnCheckedChangeListener { _, checkedId ->
                    run {
                        SharedPreferenceHelper.putInt(
                            Constants.CHATROOM_SORT_TYPE_KEY, when (checkedId) {
                                R.id.radio_sort_alphabetical -> 0
                                R.id.radio_sort_activity -> 1
                                else -> 1
                            }
                        )
                    }
                }

                groupByTypeCheckBox.isChecked = groupByType
                groupByTypeCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    SharedPreferenceHelper.putBoolean(
                        Constants.CHATROOM_GROUP_BY_TYPE_KEY,
                        isChecked
                    )
                }

                AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_sort_title)
                    .setView(dialogLayout)
                    .setPositiveButton(R.string.dialog_button_done) { dialog, _ ->
                        invalidateQueryOnSearch()
                        updateSort()
                        dialog.dismiss()
                    }.show()
            }
        }
        
        if (Constants.WIDECHAT) {
            when (item.itemId) {
                R.id.action_settings -> {
                    searchView?.clearFocus()
                    val newFragment = SettingsFragment()
                    val fragmentManager = fragmentManager
                    val fragmentTransaction = fragmentManager!!.beginTransaction()
                    fragmentTransaction.replace(R.id.fragment_container, newFragment)
                    fragmentTransaction.addToBackStack(null)
                    fragmentTransaction.commit()
                    profileButton?.setVisibility(View.GONE)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateSort() {
        val sortType = SharedPreferenceHelper.getInt(
            Constants.CHATROOM_SORT_TYPE_KEY,
            ChatRoomsSortOrder.ACTIVITY
        )
        val grouped = SharedPreferenceHelper.getBoolean(Constants.CHATROOM_GROUP_BY_TYPE_KEY, false)

        val query = when (sortType) {
            ChatRoomsSortOrder.ALPHABETICAL -> {
                Query.ByName(grouped)
            }
            ChatRoomsSortOrder.ACTIVITY -> {
                Query.ByActivity(grouped)
            }
            else -> Query.ByActivity()
        }

        viewModel.setQuery(query)
    }

    private fun invalidateQueryOnSearch() {
        searchView?.let {
            if (!searchView!!.isIconified) {
                queryChatRoomsByName(searchView!!.query.toString())
            }
        }
    }

    private fun showNoChatRoomsToDisplay() {
        ui { text_no_data_to_display.isVisible = true }
    }

    override fun showLoading() {
        view_loading.isVisible = true
    }

    override fun hideLoading() {
        view_loading.isVisible = false
    }

    override fun showMessage(resId: Int) {
        ui {
            showToast(resId)
        }
    }

    override fun showMessage(message: String) {
        ui {
            showToast(message)
        }
    }

    override fun showGenericErrorMessage() = showMessage(getString(R.string.msg_generic_error))

    override fun showLoadingRoom(name: CharSequence) {
        ui {
            progressDialog = ProgressDialog.show(activity, "Rocket.Chat", "Loading room $name")
        }
    }

    override fun hideLoadingRoom() {
        progressDialog?.dismiss()
    }

    private fun showConnectionState(state: State) {
        Timber.d("Got new state: $state")
        ui {
            text_connection_status.fadeIn()
            handler.removeCallbacks(dismissStatus)
            when (state) {
                is State.Connected -> {
                    text_connection_status.text = getString(R.string.status_connected)
                    handler.postDelayed(dismissStatus, 2000)
                }
                is State.Disconnected -> text_connection_status.text =
                        getString(R.string.status_disconnected)
                is State.Connecting -> text_connection_status.text =
                        getString(R.string.status_connecting)
                is State.Authenticating -> text_connection_status.text =
                        getString(R.string.status_authenticating)
                is State.Disconnecting -> text_connection_status.text =
                        getString(R.string.status_disconnecting)
                is State.Waiting -> text_connection_status.text =
                        getString(R.string.status_waiting, state.seconds)
            }
        }
    }

    private val dismissStatus = {
        if (text_connection_status != null) {
            text_connection_status.fadeOut()
        }
    }

    private fun setupToolbar() {
        if (Constants.WIDECHAT) {
            with((activity as MainActivity).toolbar) {
                setNavigationIcon(null)
                title = null
            }

            //val myselfName = userHelper?.user()?.name ?: ""
            //val serverUrl = serverInteractor?.get()!!
            //val avatarUrl = serverUrl.avatarUrl(myselfName)

            // WIDECHAT - custom layout for profile button
            with((activity as AppCompatActivity?)?.supportActionBar) {
                this?.setDisplayShowCustomEnabled(true)
                this?.setDisplayShowTitleEnabled(false)
                this?.setCustomView(R.layout.custom_veranda_appbar_layout)
                
                profileButton = this?.getCustomView()?.findViewById(R.id.action_profile)
                profileButton?.setOnClickListener { v ->
                        val newFragment = ProfileFragment()
                        val fragmentManager = fragmentManager
                        val fragmentTransaction = fragmentManager!!.beginTransaction()
                        fragmentTransaction.replace(R.id.fragment_container, newFragment)
                        fragmentTransaction.addToBackStack(null)
                        fragmentTransaction.commit()
                        profileButton?.setVisibility(View.GONE)
                    }

                //searchView2 = this?.getCustomView()?.findViewById(R.id.action_search)
                //searchView2?.setOnQueryTextListener { queryChatRoomsByName(this) }



                    //searchView2 = LayoutInflater.from(context).inflate(R.layout.custom_veranda_appbar_layout, null)
                    //searchView2.setBackgroundColor(Color.WHITE)
                    //searchView2.setIconified(false)
                    //searchView2.clearFocus()
                    //searchView2?.onActionViewExpanded(true)
                    //this?.setCustomView(searchView2)

                }


            } else {
                (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.title_chats)
        }
    }

    private fun queryChatRoomsByName(name: String?): Boolean {
        if (name.isNullOrEmpty()) {
            updateSort()
        } else {
            viewModel.setQuery(Query.Search(name!!))
        }
        return true
    }
}
