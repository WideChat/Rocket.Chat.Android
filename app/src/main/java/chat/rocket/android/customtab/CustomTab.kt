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
        Log.e("Debin_1000", "url = " + url)
        val uri = Uri.parse(CustomTabsHelper.convertSchemeToLower(url))
        Log.e("Debin_1001", "uri = " + uri)

        val customTabIntentBuilder = CustomTabsIntent.Builder()
        // debin added: customTabIntentBuilder.setInstantAppsEnabled(true)
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
        Log.e("Debin 1002", "Debin-4")
        val packageName = CustomTabsHelper.getPackageNameToUse(context)
        Log.e("Debin 1003: packageName: ", packageName)

        if (packageName == null) {
            Log.e("Debin 1004", "fail to get pkg name")
            fallback?.openUri(context, uri)
        } else {
            customTabIntent.intent.`package` = packageName
            Log.e("Debin 1005", "set intent package name, launching url")
            CustomTabsIntent.setAlwaysUseBrowserUI(customTabIntent.intent)
            Log.e("Debin 1005.1", "set intent package name, launching url")
            customTabIntent.launchUrl(context, uri)
            val shouldAlwaysUseBrowserUI: Boolean = CustomTabsIntent.shouldAlwaysUseBrowserUI(customTabIntent.intent)
            Log.e("Debin 1006", if (shouldAlwaysUseBrowserUI) {"yes"} else {"no"}) 
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