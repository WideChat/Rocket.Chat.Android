package chat.rocket.android.helper

import android.content.Context
import android.content.Intent
import chat.rocket.android.R
import chat.rocket.android.dynamiclinks.DynamicLinksForFirebase
import chat.rocket.android.server.domain.GetAccountInteractor
import chat.rocket.android.server.domain.GetCurrentServerInteractor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ShareAppHelper @Inject constructor(
	private val dynamicLinksManager: DynamicLinksForFirebase,
	private val serverInteractor: GetCurrentServerInteractor,
	private val getAccountInteractor: GetAccountInteractor
) {
	fun shareViaApp(context: Context) {
		GlobalScope.launch {
			//get serverUrl and username
			val server = serverInteractor.get()!!
			val account = getAccountInteractor.get(server)!!
			val userName = account.userName

			val deepLinkCallback = { returnedString: String? ->
				with(Intent(Intent.ACTION_SEND)) {
					type = "text/plain"
					putExtra(Intent.EXTRA_SUBJECT, String.format(context.getString(R.string.default_invitation_subject), userName))
					putExtra(Intent.EXTRA_TEXT, String.format(context.getString(R.string.default_invitation_text), returnedString))
					context.startActivity(Intent.createChooser(this, context.getString(R.string.msg_share_using)))
				}
			}
			dynamicLinksManager.createDynamicLink(userName, server, deepLinkCallback)
		}
	}

}