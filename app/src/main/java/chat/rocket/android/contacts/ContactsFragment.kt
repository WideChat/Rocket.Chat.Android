package chat.rocket.android.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import chat.rocket.android.R
import chat.rocket.android.contacts.models.Contact
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.util.extension.onQueryTextListener
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.app_bar.*
import java.util.ArrayList
import kotlin.collections.HashMap

// WIDECHAT
import chat.rocket.android.helper.Constants
import chat.rocket.android.server.domain.GetAccountInteractor
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import com.facebook.drawee.view.SimpleDraweeView
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.rocket.android.chatrooms.adapter.ItemHolder
import chat.rocket.android.contacts.adapter.ContactHeaderItemHolder
import chat.rocket.android.contacts.adapter.ContactItemHolder
import chat.rocket.android.contacts.adapter.ContactRecyclerViewAdapter
import chat.rocket.android.contacts.adapter.inviteItemHolder
import chat.rocket.android.db.DatabaseManagerFactory
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import chat.rocket.android.util.extensions.avatarUrl
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import kotlinx.android.synthetic.main.fragment_contact_parent.view.*
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

/**
 * Load a list of contacts in a recycler view
 */
class ContactsFragment : Fragment() {

	@Inject
	lateinit var serverInteractor: GetCurrentServerInteractor

	@Inject
	lateinit var getAccountInteractor: GetAccountInteractor

	/**
    @Inject
    lateinit var dbFactory: DatabaseManagerFactory
    @Inject
    lateinit var serverInteractor: GetCurrentServerInteractor
    private var recyclerView :RecyclerView? = null
    private var emptyTextView:  TextView? = null

    private val MY_PERMISSIONS_REQUEST_RW_CONTACTS = 0

    /**
     * The list of contacts to load in the recycler view
     */
    private var contactArrayList: ArrayList<Contact> = ArrayList()

    /**
     *  The mapping of contacts with their registration status
     */
    private var contactHashMap: HashMap<String, String> = HashMap()

    private val MY_PERMISSIONS_REQUEST_RW_CONTACTS = 0

    private var createNewChannelLink: View? = null
    private var shareViaAnotherApp: View? = null
    private var searchView: SearchView? = null
    private var sortView: MenuItem? = null
    private var searchIcon: ImageView? = null
    private var searchText:  TextView? = null
    private var searchCloseButton: ImageView? = null

    // WIDECHAT
    private var profileButton: SimpleDraweeView? = null
    private var widechatSearchView: SearchView? = null
    private var onlineStatusButton: ImageView? = null

    companion object {
        /**
         * Create a new ContactList fragment that displays the given list of contacts
         *
         * @param contactArrayList the list of contacts to load in the recycler view
         * @param contactHashMap the mapping of contacts with their registration status
         * @return the newly created ContactList fragment
         */
        fun newInstance(
                contactArrayList: ArrayList<Contact>,
                contactHashMap: HashMap<String, String>
        ): ContactsFragment {
            val contactsFragment = ContactsFragment()

            val arguments = Bundle()
            arguments.putParcelableArrayList("CONTACT_ARRAY_LIST", contactArrayList)
            arguments.putSerializable("CONTACT_HASH_MAP", contactHashMap)

            contactsFragment.arguments = arguments
            return contactsFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        // clone the inflater using the ContextThemeWrapper
        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        val view = localInflater.inflate(R.layout.fragment_contact_parent, container, false)

        this.recyclerView = view.findViewById(R.id.recycler_view)
        this.emptyTextView = view.findViewById(R.id.text_no_data_to_display)
        getContactsPermissions()
        
        shareViaAnotherApp = view.findViewById(R.id.share_via_another_app)
		shareViaAnotherApp!!.setOnClickListener { _ ->

			FirebaseDynamicLinks.getInstance().createDynamicLink()
				.setLink(Uri.parse("https://open.rocket.chat/direct/Shailesh351"))
				.setDomainUriPrefix("https://dylinktesting.page.link")
				.setAndroidParameters(
					DynamicLink.AndroidParameters.Builder("chat.veranda.android.dev").build())
				.setSocialMetaTagParameters(
					DynamicLink.SocialMetaTagParameters.Builder()
						.setTitle("Shailesh351")
						.setDescription("Chat with Shailesh on Rocket.Chat")
						.build())
				.buildShortDynamicLink(ShortDynamicLink.Suffix.SHORT)
				.addOnSuccessListener { result ->
					val link = result.shortLink.toString()
					Toast.makeText(context, link, Toast.LENGTH_SHORT).show()

					val shareIntent = Intent()
					shareIntent.action = Intent.ACTION_SEND
					shareIntent.putExtra(Intent.EXTRA_TEXT, "Default Invitation Text : $link")
					shareIntent.type = "text/plain"
					startActivity(shareIntent, null)

				}.addOnFailureListener {
					// Error
					Toast.makeText(context, "Error dynamic link", Toast.LENGTH_SHORT).show()
				}
		}
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
    }

    private fun getContactList() {
        val serverUrl = serverInteractor.get()!!
        val dbManager = dbFactory.create(serverUrl)

        Single.fromCallable {
            // need to return a non-null object, since Rx 2 doesn't allow nulls
            dbManager.contactsDao().getAllSync()
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onSuccess = { contactEntities ->
                            contactArrayList = ArrayList(contactEntities.map { contactEntity ->
                                run{
                                    val contact = Contact()
                                    contact.setName(contactEntity.name!!)
                                    if (contactEntity.isPhone) {
                                        contact.setPhoneNumber(contactEntity.phoneNumber!!)
                                        contact.setIsPhone(true)
                                    } else {
                                        contact.setEmailAddress(contactEntity.emailAddress!!)
                                    }
                                    if(contactEntity.username != null) {
                                        contact.setUsername(contactEntity.username)
                                    }
                                    contact.setAvatarUrl(serverUrl.avatarUrl(contact?.getUsername() ?: ""))
                                    contact
                                }
                            })
                            setupFrameLayout(contactArrayList)
                        },
                        onError = { error ->
                        }
                )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chatrooms, menu)

        sortView = menu.findItem(R.id.action_sort)
        sortView!!.isVisible = false

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem?.actionView as? SearchView
        searchView?.onQueryTextListener { queryContacts(it) }

        if (Constants.WIDECHAT) {
            setupWidechatSearchView()
            return
        }

        searchView?.maxWidth = Integer.MAX_VALUE
        val expandListener = object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // Simply setting sortView to visible won't work, so we invalidate the options
                // to recreate the entire menu...
                activity?.invalidateOptionsMenu()
                queryContacts("")
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }
        }
        searchItem?.setOnActionExpandListener(expandListener)
    }

    fun setupToolbar(){
        (activity as MainActivity).toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        (activity as MainActivity).toolbar.setNavigationOnClickListener { activity?.onBackPressed()}
        with((activity as AppCompatActivity?)?.supportActionBar) {
            this?.setDisplayShowTitleEnabled(true)
            this?.title = getString(R.string.title_contacts)
        }

        if (Constants.WIDECHAT) {
            with((activity as AppCompatActivity?)?.supportActionBar) {
                profileButton = this?.getCustomView()?.findViewById(R.id.profile_image_avatar)
                profileButton?.visibility = View.GONE
                onlineStatusButton=this?.getCustomView()?.findViewById(R.id.text_online)
                onlineStatusButton?.visibility = View.GONE
                widechatSearchView = this?.getCustomView()?.findViewById(R.id.action_widechat_search)
                widechatSearchView?.visibility = View.GONE
            }
        }
    }

    private fun setupWidechatSearchView() {
        searchView?.setBackgroundResource(R.drawable.widechat_search_white_background)
        searchView?.isIconified = true

        searchIcon = searchView?.findViewById(R.id.search_mag_icon)
        searchIcon?.setImageResource(R.drawable.ic_search_gray_24px)

        searchText = searchView?.findViewById(R.id.search_src_text)
        searchText?.setTextColor(Color.GRAY)
        searchText?.setHintTextColor(Color.GRAY)

        searchCloseButton = searchView?.findViewById(R.id.search_close_btn)
        searchCloseButton?.setImageResource(R.drawable.ic_close_gray_24dp)

        searchCloseButton?.setOnClickListener { v ->
            searchView?.clearFocus()
            searchView?.setQuery("", false)
        }
    }

    fun containsIgnoreCase(src: String, what: String): Boolean {
        val length = what.length
        if (length == 0)
            return true // Empty string is contained

        val firstLo = Character.toLowerCase(what[0])
        val firstUp = Character.toUpperCase(what[0])

        for (i in src.length - length downTo 0) {
            // Quick check before calling the more expensive regionMatches() method:
            val ch = src[i]
            if (ch != firstLo && ch != firstUp)
                continue

            if (src.regionMatches(i, what, 0, length, ignoreCase = true))
                return true
        }
        return false
    }

    fun queryContacts(query: String) {
        if (query.isBlank() or query.isEmpty()) {
            setupFrameLayout(contactArrayList)
        } else {
            var filteredContactArrayList: ArrayList<Contact> = ArrayList()
            for (contact in contactArrayList) {
                if (containsIgnoreCase(contact.getName()!!, query)) {
                    filteredContactArrayList.add(contact)
                }
            }
            setupFrameLayout(filteredContactArrayList)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RW_CONTACTS -> {
                if (
                        grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission granted
                    getContactList()
                }
                setupFrameLayout(contactArrayList)
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun getContactsPermissions() {
        if (
                ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        ) {
            getContactList()
            setupFrameLayout(contactArrayList)
        } else {
            requestPermissions(
                    arrayOf(
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_CONTACTS
                    ),
                    MY_PERMISSIONS_REQUEST_RW_CONTACTS
            )
        }
    }

    fun setupFrameLayout(filteredContactArrayList: ArrayList<Contact>) {

        if (filteredContactArrayList!!.size == 0) {
            emptyTextView!!.visibility = View.VISIBLE
            recyclerView!!.visibility = View.GONE
        } else {
            emptyTextView!!.visibility = View.GONE
            recyclerView!!.visibility = View.VISIBLE

            recyclerView!!.setHasFixedSize(true)
            recyclerView!!.layoutManager = LinearLayoutManager(context)
            recyclerView!!.adapter = ContactRecyclerViewAdapter(this.activity as MainActivity, map(filteredContactArrayList)!!)
        }
    }

    fun map(contacts: List<Contact>): ArrayList<ItemHolder<*>> {
        val finalList = ArrayList<ItemHolder<*>>(contacts.size + 2)
        val userList = ArrayList<ItemHolder<*>>(contacts.size)
        val userContactList: ArrayList<Contact> = ArrayList()
        val contactsList = ArrayList<ItemHolder<*>>(contacts.size)
        contacts.forEach { contact ->
            if(contact.getUsername()!= null){
                // Users in their own list for filtering before adding to an ItemHolder
                userContactList.add(contact)
            } else {
                contactsList.add(ContactItemHolder(contact))
            }
        }
        // Filter for dupes
        userContactList.distinctBy { it.getUsername() }.forEach { contact ->
            userList.add(ContactItemHolder(contact))
        }

        finalList.addAll(userList)
        finalList.add(ContactHeaderItemHolder("INVITE CONTACTS"))
        finalList.addAll(contactsList)
        finalList.add(inviteItemHolder("invite"))
        return finalList
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_contact_parent, container, false)

        createNewChannelLink = view.findViewById(R.id.create_new_channel_button)
        createNewChannelLink!!.setOnClickListener {
            val createChannelFragment = CreateChannelFragment()
            val transaction = activity?.supportFragmentManager?.beginTransaction();
            transaction?.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
            transaction?.replace(this.id, createChannelFragment, "createChannelFragment");
            transaction?.addToBackStack(null)?.commit();
        }

		shareViaAnotherApp = view.findViewById(R.id.share_via_another_app)
		shareViaAnotherApp!!.setOnClickListener { _ ->

			launch {
				//get serverUrl and username
				val server = serverInteractor.get()!!
				val account = getAccountInteractor.get(server)!!
				val userName = account.userName

				FirebaseDynamicLinks.getInstance().createDynamicLink()
					.setLink(Uri.parse("$server/direct/$userName"))
					.setDomainUriPrefix("https://dylinktesting.page.link")
					.setAndroidParameters(
						DynamicLink.AndroidParameters.Builder("chat.veranda.android.dev").build())
					.setSocialMetaTagParameters(
						DynamicLink.SocialMetaTagParameters.Builder()
							.setTitle(userName)
							.setDescription("Chat with $userName on Rocket.Chat")
							.build())
					.buildShortDynamicLink(ShortDynamicLink.Suffix.SHORT)
					.addOnSuccessListener { result ->
						val link = result.shortLink.toString()
						Toast.makeText(context, link, Toast.LENGTH_SHORT).show()

						val shareIntent = Intent()
						shareIntent.action = Intent.ACTION_SEND
						shareIntent.putExtra(Intent.EXTRA_TEXT, "Default Invitation Text : $link")
						shareIntent.type = "text/plain"
						startActivity(shareIntent, null)

					}.addOnFailureListener {
						// Error
						Toast.makeText(context, "Error dynamic link", Toast.LENGTH_SHORT).show()
					}
			}
		}

		return view
	}

    companion object {

        /**
         * Create a new ContactList fragment that displays the given list of contacts
         *
         * @param contactArrayList the list of contacts to load in the recycler view
         * @param contactHashMap the mapping of contacts with their registration status
         * @return the newly created ContactList fragment
         */
        fun newInstance(
                contactArrayList: ArrayList<Contact>,
                contactHashMap: HashMap<String, String>
        ): ContactsFragment {
            val contactsFragment = ContactsFragment()

            val arguments = Bundle()
            arguments.putParcelableArrayList("CONTACT_ARRAY_LIST", contactArrayList)
            arguments.putSerializable("CONTACT_HASH_MAP", contactHashMap)

            contactsFragment.arguments = arguments

            return contactsFragment
        }
    }

}
