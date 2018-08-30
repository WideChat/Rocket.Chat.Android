package chat.rocket.android.customtab

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.res.ResourcesCompat
import chat.rocket.android.R

object CustomTab {
    fun openCustomTab(context: Context, url: String, fallback: CustomTabFallback?, setBackButton: Boolean = false) {
        val uri = Uri.parse(url)
        
        val customTabIntentBuilder = CustomTabsIntent.Builder()
        customTabIntentBuilder.setToolbarColor(ResourcesCompat.getColor(context.resources, R.color.colorPrimary, context.theme))

        //Set action on clicking bookmark
        val actionLabel = context.resources.getString(R.string.customtab_bookmark_label)
        val icon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_bookmark)
        val pendingIntent = createPendingIntent(context, ActionBroadcastReceiver.ACTION_ACTION_BUTTON)
        customTabIntentBuilder.setActionButton(icon, actionLabel, pendingIntent)
        customTabIntentBuilder.addDefaultShareMenuItem()
        customTabIntentBuilder.setShowTitle(true)

        if (setBackButton) {
           customTabIntentBuilder.setCloseButtonIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_arrow_back))
        }

        val customTabIntent = customTabIntentBuilder.build()
        val packageName = CustomTabsHelper.getPackageNameToUse(context)

        if (packageName == null) {
            fallback?.openUri(context, uri)
        } else {
            customTabIntent.intent.`package` = packageName
            // CustomTabsIntent.setAlwaysUseBrowserUI(customTabIntent.intent)
            customTabIntent.launchUrl(context, uri)
            val shouldAlwaysUseBrowserUI: Boolean = CustomTabsIntent.shouldAlwaysUseBrowserUI(customTabIntent.intent)
            Log.e("Viasat: UserBrowserUI: ", if (shouldAlwaysUseBrowserUI) {"yes"} else {"no"}) 
        }
    }

    private fun createPendingIntent(context: Context, actionSourceId: Int): PendingIntent {
        val actionIntent = Intent(context.applicationContext, ActionBroadcastReceiver::class.java)
        actionIntent.putExtra(ActionBroadcastReceiver.KEY_ACTION_SOURCE, actionSourceId)
        return PendingIntent.getBroadcast(context.applicationContext, actionSourceId, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * To be used as a fallback to open the Uri when Custom Tabs is not available.
     */
    interface CustomTabFallback {
        fun openUri(context: Context, uri: Uri)
    }
}
