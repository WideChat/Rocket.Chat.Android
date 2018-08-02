package chat.rocket.android.chatrooms.ui

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import chat.rocket.android.R
import chat.rocket.android.chatrooms.adapter.RoomsAdapter
import chat.rocket.android.chatrooms.infrastructure.ChatRoomsRepository
import chat.rocket.android.chatrooms.presentation.ChatRoomsPresenter
import chat.rocket.android.chatrooms.presentation.ChatRoomsView
import chat.rocket.android.chatrooms.viewmodel.ChatRoomsViewModel
import chat.rocket.android.chatrooms.viewmodel.ChatRoomsViewModelFactory
import chat.rocket.android.db.DatabaseManager
import chat.rocket.android.helper.ChatRoomsSortOrder
import chat.rocket.android.helper.Constants
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.showToast
import chat.rocket.android.util.extensions.ui
import chat.rocket.android.util.extensions.fadeIn
import chat.rocket.android.util.extensions.fadeOut
import chat.rocket.android.customtab.CustomTab
import chat.rocket.android.customtab.WebViewFallback
import chat.rocket.android.helper.SharedPreferenceHelper
import chat.rocket.android.room.weblink.WebLinkEntity
import chat.rocket.android.util.extensions.*
import chat.rocket.android.weblinks.presentation.WebLinksPresenter
import chat.rocket.android.weblinks.presentation.WebLinksView
import chat.rocket.android.weblinks.ui.WebLinksAdapter
import chat.rocket.android.webview.weblink.ui.webViewIntent
import chat.rocket.android.widget.DividerItemDecoration
import chat.rocket.core.internal.realtime.socket.model.State
import chat.rocket.common.model.RoomType
import chat.rocket.core.model.ChatRoom
import com.facebook.drawee.view.SimpleDraweeView
import com.leocardz.link.preview.library.LinkPreviewCallback
import com.leocardz.link.preview.library.SourceContent
import com.leocardz.link.preview.library.TextCrawler
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_chat_rooms.*
import kotlinx.android.synthetic.main.item_web_link.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import javax.inject.Inject

private const val BUNDLE_CHAT_ROOM_ID = "BUNDLE_CHAT_ROOM_ID"

class ChatRoomsFragment : Fragment(), ChatRoomsView, WebLinksView {
    @Inject
    lateinit var presenter: ChatRoomsPresenter
    @Inject
    lateinit var factory: ChatRoomsViewModelFactory
    @Inject
    lateinit var dbManager: DatabaseManager // TODO - remove when moving ChatRoom screen to DB

    lateinit var viewModel: ChatRoomsViewModel

    @Inject
    lateinit var webLinksPresenter: WebLinksPresenter

    private lateinit var preferences: SharedPreferences
    private var searchView: SearchView? = null
    private val handler = Handler()

    private var listJob: Job? = null
    private var chatRoomId: String? = null

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
        setupWebLinksRecyclerView()
        setupWebSearch()
        setupWebLinksExpandButton()
    }

    override fun onResume() {
        super.onResume()
        webLinksPresenter.loadWebLinks()
    }

    override fun onDestroyView() {
        listJob?.cancel()
        super.onDestroyView()
    }    
        
    private fun subscribeUi() {
        ui {

            val adapter = RoomsAdapter { roomId ->
                launch(UI) {
                    dbManager.getRoom(roomId)?.let { room ->
                        ui {
                            presenter.loadChatRoom(room)
                        }
                    }

                }
            }

            recycler_view.layoutManager = LinearLayoutManager(it)
            recycler_view.addItemDecoration(DividerItemDecoration(it,
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_start),
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_end)))
            recycler_view.itemAnimator = DefaultItemAnimator()
            recycler_view.adapter = adapter

            viewModel.getChatRooms().observe(viewLifecycleOwner, Observer { rooms ->
                rooms?.let {
                    Timber.d("Got items: $it")
                    adapter.values = it
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

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as? SearchView
        searchView?.setIconifiedByDefault(false)
        searchView?.maxWidth = Integer.MAX_VALUE
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return queryChatRoomsByName(query)
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return queryChatRoomsByName(newText)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        // TODO - simplify this
            R.id.action_sort -> {
                val dialogLayout = layoutInflater.inflate(R.layout.chatroom_sort_dialog, null)
                val sortType = SharedPreferenceHelper.getInt(Constants.CHATROOM_SORT_TYPE_KEY, ChatRoomsSortOrder.ACTIVITY)
                val groupByType = SharedPreferenceHelper.getBoolean(Constants.CHATROOM_GROUP_BY_TYPE_KEY, false)

                val radioGroup = dialogLayout.findViewById<RadioGroup>(R.id.radio_group_sort)
                val groupByTypeCheckBox = dialogLayout.findViewById<CheckBox>(R.id.checkbox_group_by_type)

                radioGroup.check(when (sortType) {
                    0 -> R.id.radio_sort_alphabetical
                    else -> R.id.radio_sort_activity
                })
                radioGroup.setOnCheckedChangeListener { _, checkedId ->
                    run {
                        SharedPreferenceHelper.putInt(Constants.CHATROOM_SORT_TYPE_KEY, when (checkedId) {
                            R.id.radio_sort_alphabetical -> 0
                            R.id.radio_sort_activity -> 1
                            else -> 1
                        })
                    }
                }

                groupByTypeCheckBox.isChecked = groupByType
                groupByTypeCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    SharedPreferenceHelper.putBoolean(Constants.CHATROOM_GROUP_BY_TYPE_KEY, isChecked)
                }

                AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_sort_title)
                    .setView(dialogLayout)
                    .setPositiveButton("Done") { dialog, _ ->
                        invalidateQueryOnSearch()
                        updateSort()
                        dialog.dismiss()
                    }.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateSort() {
        val sortType = SharedPreferenceHelper.getInt(Constants.CHATROOM_SORT_TYPE_KEY, ChatRoomsSortOrder.ACTIVITY)
        val grouped = SharedPreferenceHelper.getBoolean(Constants.CHATROOM_GROUP_BY_TYPE_KEY, false)

        val order = when (sortType) {
            ChatRoomsSortOrder.ALPHABETICAL -> {
                if (grouped) {
                    ChatRoomsRepository.Order.GROUPED_NAME
                } else {
                    ChatRoomsRepository.Order.NAME
                }
            }
            ChatRoomsSortOrder.ACTIVITY -> {
                if (grouped) {
                    ChatRoomsRepository.Order.GROUPED_ACTIVITY
                } else {
                    ChatRoomsRepository.Order.ACTIVITY
                }
            }
            else -> ChatRoomsRepository.Order.ACTIVITY
        }

        viewModel.setOrdering(order)
    }

    private fun invalidateQueryOnSearch() {
        searchView?.let {
            if (!searchView!!.isIconified) {
                queryChatRoomsByName(searchView!!.query.toString())
            }
        }
    }

    override suspend fun updateChatRooms(newDataSet: List<ChatRoom>) {}

    override fun showNoChatRoomsToDisplay() {
        ui { text_no_data_to_display.isVisible = true }
    }

    override fun showLoading() {
        ui { view_loading.isVisible = true }
    }

    override fun hideLoading() {
        ui {
            view_loading.isVisible = false
        }
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

    override fun showConnectionState(state: State) {
        Timber.d("Got new state: $state")
        ui {
            connection_status_text.fadeIn()
            handler.removeCallbacks(dismissStatus)
            when (state) {
                is State.Connected -> {
                    connection_status_text.text = getString(R.string.status_connected)
                    handler.postDelayed(dismissStatus, 2000)
                }
                is State.Disconnected -> connection_status_text.text = getString(R.string.status_disconnected)
                is State.Connecting -> connection_status_text.text = getString(R.string.status_connecting)
                is State.Authenticating -> connection_status_text.text = getString(R.string.status_authenticating)
                is State.Disconnecting -> connection_status_text.text = getString(R.string.status_disconnecting)
                is State.Waiting -> connection_status_text.text = getString(R.string.status_waiting, state.seconds)
            }
        }
    }

    private val dismissStatus = {
        if (connection_status_text != null) {
            connection_status_text.fadeOut()
        }
    }

    override suspend fun updateWebLinks(newDataSet: List<WebLinkEntity>) {

        if (!newDataSet.isEmpty()){
            web_links_expand_button.visibility = View.VISIBLE
        }

        activity?.apply {
            listJob?.cancel()
            listJob = launch(UI) {
                val adapter = web_links_recycler_view.adapter as WebLinksAdapter
                if (isActive) {
                    adapter.updateWebLinks(newDataSet)
                }
            }
        }
    }

    override fun showNoWebLinksToDisplay() {
        val adapter = web_links_recycler_view.adapter as WebLinksAdapter
        adapter.clearData()
        web_links_expand_button.visibility = View.GONE
        divider_web_links.visibility = View.GONE
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.title_chats)
    }

    private fun setupWebLinksRecyclerView() {
        ui {
            web_links_recycler_view.layoutManager = LinearLayoutManager(it, LinearLayoutManager.VERTICAL, false)
            web_links_recycler_view.addItemDecoration(DividerItemDecoration(it,
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_start),
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_end)))
            web_links_recycler_view.itemAnimator = DefaultItemAnimator()

            web_links_recycler_view.adapter = WebLinksAdapter(it
            ) { webLink ->
                run {
                    startActivity(it.webViewIntent(webLink.link, if (!webLink.title.isEmpty()) webLink.title else resources.getString(R.string.url_preview_title)))
                }
            }
        }
    }

    private fun setupWebLinksExpandButton() {
        web_links_expand_button.setOnClickListener {
            if (web_links_recycler_view.isVisible()) {
                web_links_expand_button.setImageResource(R.drawable.ic_arrow_drop_down_black)
                web_links_recycler_view.visibility = View.GONE
                divider_web_links.visibility = View.GONE
            } else {
                web_links_expand_button.setImageResource(R.drawable.ic_arrow_drop_up_black)
                web_links_recycler_view.visibility = View.VISIBLE
                divider_web_links.visibility = View.VISIBLE
            }
        }
    }

    private fun setupWebSearch() {
        //val link = "http://bizzbyster.github.io/search/"

        val title = SharedPreferenceHelper.getString("web_search_title", "Internet Search")
        val description = SharedPreferenceHelper.getString("web_search_desc", "Faster web with the Viasat Browser")
        val imageUrl = SharedPreferenceHelper.getString("web_search_image", "http://www.verandaweb.com/search/browser.png")
        val link = SharedPreferenceHelper.getString("web_search_link", "https://www.google.com")

        updateUI(title, text_title,
                description, text_description,
                imageUrl, image_web_link,
                link, text_link)

        web_search.setOnClickListener {
            //CustomTab.openCustomTab(context!!, link, WebViewFallback(), true)
            startActivity(this.activity?.webViewIntent(link, if (!title.isEmpty()) title else resources.getString(R.string.url_preview_title)))
        }

        val linkPreviewCallback = object : LinkPreviewCallback {

            override fun onPre() {
                //Do nothing
            }

            override fun onPos(sourceContent: SourceContent?, b: Boolean) {
                sourceContent?.let {
                    val newTitle = sourceContent.title
                    val newDescription = sourceContent.description
                    val imageList = sourceContent.images
                    var newImageUrl = ""

                    if (imageList != null && imageList.size != 0) {
                        newImageUrl = imageList[0]
                    }

                    updateUI(newTitle, text_title,
                            newDescription, text_description,
                            newImageUrl, image_web_link,
                            link, text_link)

                    launch {
                        SharedPreferenceHelper.putString("web_search_title", newTitle)
                        SharedPreferenceHelper.putString("web_search_desc", newDescription)
                        SharedPreferenceHelper.putString("web_search_image", newImageUrl)
                        SharedPreferenceHelper.putString("web_search_link", link)
                    }
                }
            }
        }
//        val textCrawler = TextCrawler()
//        textCrawler.makePreview(linkPreviewCallback, link)
    }

    private fun updateUI(title: String, textViewTitle: TextView,
                         description: String, textViewDescription: TextView,
                         imageUrl: String, imageView: SimpleDraweeView,
                         link: String, textViewLink: TextView) {

        if (!title.isEmpty()) {
            textViewTitle.visibility = View.VISIBLE
            textViewTitle.content = title
        }

        if (!description.isEmpty()) {
            textViewDescription.visibility = View.VISIBLE
            textViewDescription.content = description
        }

        if (title.isEmpty() && !description.isEmpty()) {
            textViewDescription.visibility = View.GONE
            textViewTitle.visibility = View.VISIBLE
            textViewTitle.content = description
        }

        if (!imageUrl.isEmpty()) {
            imageView.visibility = View.VISIBLE
            imageView.setImageURI(imageUrl)
        } else {
            imageView.setActualImageResource(R.drawable.ic_link_black_24dp)
        }

        if (!link.isEmpty()) {
            textViewLink.content = link
        }
    }

    private fun queryChatRoomsByName(name: String?): Boolean {
        //presenter.chatRoomsByName(name ?: "")
        return true
    }
}