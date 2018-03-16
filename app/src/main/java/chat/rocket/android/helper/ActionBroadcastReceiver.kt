package chat.rocket.android.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import timber.log.Timber

/**
 * A BroadcastReceiver that handles the Action Intent from the Custom Tab and shows the Url
 * in a Toast.
 */
class ActionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.dataString
        if (url != null) {
            val toastText = getToastText(context, intent.getIntExtra(KEY_ACTION_SOURCE, -1), url)
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getToastText(context: Context, actionId: Int, url: String): String {
        when (actionId) {
            ACTION_ACTION_BUTTON -> return "Add Web Channel Button for $url clicked"
            ACTION_MENU_ITEM -> return "Add Web Channel Menu Item for $url clicked"
            ACTION_TOOLBAR -> return "Action Toolbar for $url"
            else -> return "Unknown Action for $url"
        }
    }

    companion object {
        val KEY_ACTION_SOURCE = "chat.rocket.android.helper.ACTION_SOURCE"
        val ACTION_ACTION_BUTTON = 1
        val ACTION_MENU_ITEM = 2
        val ACTION_TOOLBAR = 3
    }
}

