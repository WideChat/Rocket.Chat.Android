package chat.rocket.android.chatrooms.ui

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import chat.rocket.android.R
import chat.rocket.android.chatrooms.presentation.ChatRoomsPresenter
import chat.rocket.android.chatrooms.presentation.ChatRoomsView
import chat.rocket.android.helper.ChatRoomsSortOrder
import chat.rocket.android.helper.Constants
import chat.rocket.android.helper.SharedPreferenceHelper
import chat.rocket.android.infrastructure.LocalRepository
import chat.rocket.android.room.weblink.WebLinkEntity
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import chat.rocket.android.server.domain.SettingsRepository
import chat.rocket.android.util.extensions.*
import chat.rocket.android.weblinks.presentation.WebLinksPresenter
import chat.rocket.android.weblinks.presentation.WebLinksView
import chat.rocket.android.weblinks.ui.WebLinksAdapter
import chat.rocket.android.webview.weblink.ui.webViewIntent
import chat.rocket.android.widget.DividerItemDecoration
import chat.rocket.common.model.RoomType
import chat.rocket.core.internal.realtime.socket.model.State
import chat.rocket.core.model.ChatRoom
import com.facebook.drawee.view.SimpleDraweeView
import com.leocardz.link.preview.library.LinkPreviewCallback
import com.leocardz.link.preview.library.SourceContent
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_chat_rooms.*
import kotlinx.android.synthetic.main.item_web_link.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.NonCancellable.isActive
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import javax.inject.Inject


class ChatRoomsFragment : Fragment(), ChatRoomsView, WebLinksView {
    @Inject
    lateinit var presenter: ChatRoomsPresenter
    @Inject
    lateinit var serverInteractor: GetCurrentServerInteractor
    @Inject
    lateinit var settingsRepository: SettingsRepository
    @Inject
    lateinit var localRepository: LocalRepository
    @Inject
    lateinit var webLinksPresenter: WebLinksPresenter

    private lateinit var preferences: SharedPreferences
    private var searchView: SearchView? = null
    private val handler = Handler()

    private var listJob: Job? = null
    private var sectionedAdapter: SimpleSectionedRecyclerViewAdapter? = null

    companion object {
        fun newInstance() = ChatRoomsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
        setHasOptionsMenu(true)
        preferences = context?.getSharedPreferences("temp", Context.MODE_PRIVATE)!!
    }

    override fun onDestroy() {
        handler.removeCallbacks(dismissStatus)
        presenter.disconnect()
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = container?.inflate(R.layout.fragment_chat_rooms)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupWebLinksRecyclerView()
        setupWebSearch()
        setupWebLinksExpandButton()
        presenter.loadChatRooms()
    }

    override fun onResume() {
        super.onResume()
        webLinksPresenter.loadWebLinks()
    }

    override fun onDestroyView() {
        listJob?.cancel()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chatrooms, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as SearchView
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
                radioGroup.setOnCheckedChangeListener({ _, checkedId ->
                    run {
                        SharedPreferenceHelper.putInt(Constants.CHATROOM_SORT_TYPE_KEY, when (checkedId) {
                            R.id.radio_sort_alphabetical -> 0
                            R.id.radio_sort_activity -> 1
                            else -> 1
                        })
                        presenter.updateSortedChatRooms()
                        invalidateQueryOnSearch()
                    }
                })

                groupByTypeCheckBox.isChecked = groupByType
                groupByTypeCheckBox.setOnCheckedChangeListener({ _, isChecked ->
                    SharedPreferenceHelper.putBoolean(Constants.CHATROOM_GROUP_BY_TYPE_KEY, isChecked)
                    presenter.updateSortedChatRooms()
                    invalidateQueryOnSearch()
                })

                val dialogSort = AlertDialog.Builder(context)
                        .setTitle(R.string.dialog_sort_title)
                        .setView(dialogLayout)
                        .setPositiveButton("Done", { dialog, _ -> dialog.dismiss() })

                dialogSort.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun invalidateQueryOnSearch() {
        searchView?.let {
            if (!searchView!!.isIconified) {
                queryChatRoomsByName(searchView!!.query.toString())
            }
        }
    }

    override suspend fun updateChatRooms(newDataSet: List<ChatRoom>) {
        listJob?.cancel()
        listJob = ui {
            val adapter = recycler_view.adapter as SimpleSectionedRecyclerViewAdapter
            // FIXME https://fabric.io/rocketchat3/android/apps/chat.rocket.android/issues/5ac2916c36c7b235275ccccf
            // TODO - fix this bug to re-enable DiffUtil
            /*val diff = async(CommonPool) {
                DiffUtil.calculateDiff(RoomsDiffCallback(adapter.baseAdapter.dataSet, newDataSet))
            }.await()*/

            if (isActive) {
                adapter.baseAdapter.updateRooms(newDataSet)
                // TODO - fix crash to re-enable diff.dispatchUpdatesTo(adapter)
                adapter.notifyDataSetChanged()

                //Set sections always after data set is updated
                setSections()
            }
        }
    }

    override fun showNoChatRoomsToDisplay() {
        ui { text_no_data_to_display.setVisible(true) }
    }

    override fun showLoading() {
        ui { view_loading.setVisible(true) }
    }

    override fun hideLoading() {
        ui {
            view_loading.setVisible(false)
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
        if (!newDataSet.isEmpty()) {
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

    private fun setupRecyclerView() {
        ui {
            recycler_view.layoutManager = LinearLayoutManager(it, LinearLayoutManager.VERTICAL, false)
            recycler_view.addItemDecoration(DividerItemDecoration(it,
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_start),
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_end)))
            recycler_view.itemAnimator = DefaultItemAnimator() as RecyclerView.ItemAnimator?
            // TODO - use a ViewModel Mapper instead of using settings on the adapter

            val baseAdapter = ChatRoomsAdapter(it,
                    settingsRepository.get(serverInteractor.get()!!), localRepository) { chatRoom ->
                presenter.loadChatRoom(chatRoom)
            }

            sectionedAdapter = SimpleSectionedRecyclerViewAdapter(it,
                    R.layout.item_chatroom_header, R.id.text_chatroom_header, baseAdapter)
            recycler_view.adapter = sectionedAdapter
        }
    }

    private fun setupWebLinksRecyclerView() {
        activity?.apply {
            web_links_recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            web_links_recycler_view.addItemDecoration(DividerItemDecoration(this,
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_start),
                    resources.getDimensionPixelSize(R.dimen.divider_item_decorator_bound_end)))
            web_links_recycler_view.itemAnimator = DefaultItemAnimator()

            web_links_recycler_view.adapter = WebLinksAdapter(this,
                    { webLink ->
                        run {
                            startActivity(this.webViewIntent(webLink.link, if (!webLink.title.isEmpty()) webLink.title else resources.getString(R.string.url_preview_title)))
                        }
                    })
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

    private fun setSections() {
        //Don't add section if not grouping by RoomType
        if (!SharedPreferenceHelper.getBoolean(Constants.CHATROOM_GROUP_BY_TYPE_KEY, false)) {
            sectionedAdapter?.clearSections()
            return
        }

        val sections = ArrayList<SimpleSectionedRecyclerViewAdapter.Section>()

        sectionedAdapter?.baseAdapter?.dataSet?.let {
            var previousChatRoomType = ""

            for ((position, chatRoom) in it.withIndex()) {
                val type = chatRoom.type.toString()
                if (type != previousChatRoomType) {
                    val title = when (type) {
                        RoomType.CHANNEL.toString() -> resources.getString(R.string.header_channel)
                        RoomType.PRIVATE_GROUP.toString() -> resources.getString(R.string.header_private_groups)
                        RoomType.DIRECT_MESSAGE.toString() -> resources.getString(R.string.header_direct_messages)
                        RoomType.LIVECHAT.toString() -> resources.getString(R.string.header_live_chats)
                        else -> resources.getString(R.string.header_unknown)
                    }
                    sections.add(SimpleSectionedRecyclerViewAdapter.Section(position, title))
                }
                previousChatRoomType = chatRoom.type.toString()
            }
        }

        val dummy = arrayOfNulls<SimpleSectionedRecyclerViewAdapter.Section>(sections.size)
        sectionedAdapter?.setSections(sections.toArray(dummy))
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

        web_search.setOnClickListener({
            //CustomTab.openCustomTab(context!!, link, WebViewFallback(), true)
            startActivity(this.activity?.webViewIntent(link, if (!title.isEmpty()) title else resources.getString(R.string.url_preview_title)))
        })

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
        presenter.chatRoomsByName(name ?: "")
        return true
    }

    class RoomsDiffCallback(private val oldRooms: List<ChatRoom>,
                            private val newRooms: List<ChatRoom>) : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldRooms[oldItemPosition].id == newRooms[newItemPosition].id
        }

        override fun getOldListSize(): Int {
            return oldRooms.size
        }

        override fun getNewListSize(): Int {
            return newRooms.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldRooms[oldItemPosition].updatedAt == newRooms[newItemPosition].updatedAt
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return newRooms[newItemPosition]
        }
    }
}