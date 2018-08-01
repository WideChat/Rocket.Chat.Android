package chat.rocket.android.customtab

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import java.util.*
import okhttp3.HttpUrl

/**
 * Helper class for Custom Tabs.
 */
object CustomTabsHelper {
    private const val TAG = "CustomTabsHelper"
    private const val STABLE_PACKAGE = "com.android.chrome"
    private const val BETA_PACKAGE = "com.chrome.beta"
    private const val DEV_PACKAGE = "com.chrome.dev"
    private const val LOCAL_PACKAGE = "com.google.android.apps.chrome"
    private const val VB_PACKAGE = "com.viasat.browser"
    private const val EXTRA_CUSTOM_TABS_KEEP_ALIVE = "android.support.customtabs.extra.KEEP_ALIVE"
    private const val ACTION_CUSTOM_TABS_CONNECTION = "android.support.customtabs.action.CustomTabsService"

    private var sPackageNameToUse: String? = null

    /**
     * @return All possible chrome package names that provide custom tabs feature.
     */
    val packages: Array<String>
        get() = arrayOf("", STABLE_PACKAGE, BETA_PACKAGE, DEV_PACKAGE, LOCAL_PACKAGE)

    /**
     * Lower case the scheme to avoid a custom tabs bug.
     */
    fun convertSchemeToLower(url: String): String {

        val link = if (url.toLowerCase().matches("^\\w+?://.*".toRegex())) url else "http://" + url 

        val httpUrl = HttpUrl.parse(link)

        if (httpUrl != null) {
            val scheme = httpUrl?.scheme()?.toLowerCase()
       
            return httpUrl?.newBuilder()
                ?.scheme(scheme)
                ?.build().toString()
        } else {
            return link
        }
    }

    fun postToastMessage(context: Context, msg: String): Unit {
        var toast = Toast.makeText(context,
                msg, Toast.LENGTH_LONG)
        var view: View = toast.getView()
        view.setBackgroundColor(Color.CYAN);
        val pos = Gravity.TOP or Gravity.CENTER
        toast.setGravity(pos, 0, 300)
        toast.show()
    }

    /**
     * Goes through all apps that handle VIEW intents and have a warmup service. Picks
     * the one chosen by the user if there is one, otherwise makes a best effort to return a
     * valid package name.
     *
     *
     * This is **not** threadsafe.
     *
     * @param context [Context] to use for accessing [PackageManager].
     * @return The package name recommended to use for connecting to custom tabs related components.
     */
    fun getPackageNameToUse(context: Context): String? {
        /*val logger = LoggerFactory.getLogger(ThisClass::class)

        logger.info(“Hi")
        */
        //val Log = Logger.getLogger(MainActivity::class.java.name)
        //Log.warning("Hello World Debin")
        Log.e("Debin", "Debin-1")
        var foundVbPackage: Boolean = false

        if (sPackageNameToUse != null) {
            Log.e("Debin-100",
                    "Ignored value: sPackangeNameToUse is not empty: " + sPackageNameToUse)
            return sPackageNameToUse
        }

        val pm = context.packageManager
        // Get default VIEW intent handler.
         val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
        //val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("chrome://newtab"))
        // val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://browser.viasat.com/newnewnewtab/index.html"))
        val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
        var defaultViewHandlerPackageName: String? = null
        if (defaultViewHandlerInfo != null) {
            defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName
            Log.e("Debin-101",
                    "defaultViewHandlerPackageName=" + defaultViewHandlerPackageName)
        } else {
            Log.e ("Debin-102", "No default view handler info")
        }

        // Get all apps that can handle VIEW intents.
        val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)
        val packagesSupportingCustomTabs = ArrayList<String>()
        for (info in resolvedActivityList) {
            val serviceIntent = Intent()
            serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
            serviceIntent.`package` = info.activityInfo.packageName
            if (pm.resolveService(serviceIntent, 0) != null) {
                Log.e("Debin-103", "adding package: " + info.activityInfo.packageName)
                packagesSupportingCustomTabs.add(info.activityInfo.packageName)
            }
        }

        // Now packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
        // and service calls.
        if (packagesSupportingCustomTabs.isEmpty()) {
            Log.e("Debin-104", "No package found")
            sPackageNameToUse = null
        } else if (packagesSupportingCustomTabs.contains(VB_PACKAGE)){
            sPackageNameToUse = VB_PACKAGE
            postToastMessage(context, "You are using Viasat Browser as search platform")
            Log.e("Debin-104.5", "Use VB package")
        } else if (packagesSupportingCustomTabs.size == 1) {
            Log.e("Debin-105", "found single packet: " + packagesSupportingCustomTabs[0])
            sPackageNameToUse = packagesSupportingCustomTabs[0]
        } else if (!TextUtils.isEmpty(defaultViewHandlerPackageName)
                && !hasSpecializedHandlerIntents(context, activityIntent)
                && packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)) {
            sPackageNameToUse = defaultViewHandlerPackageName
            Log.e("Debin-106", "Use default packet: " + sPackageNameToUse)
        } else if (packagesSupportingCustomTabs.contains(STABLE_PACKAGE)) {
            sPackageNameToUse = STABLE_PACKAGE
            Log.e("Debin-107", "Use stable package: " + sPackageNameToUse)
        } else if (packagesSupportingCustomTabs.contains(BETA_PACKAGE)) {
            sPackageNameToUse = BETA_PACKAGE
            Log.e("Debin-108", "Use Beta package: " + sPackageNameToUse)
        } else if (packagesSupportingCustomTabs.contains(DEV_PACKAGE)) {
            sPackageNameToUse = DEV_PACKAGE
            Log.e("Debin-109", "Use Dev package: " + sPackageNameToUse)
        } else if (packagesSupportingCustomTabs.contains(LOCAL_PACKAGE)) {
            sPackageNameToUse = LOCAL_PACKAGE
            Log.e("Debin-110", "Use Local package: " + sPackageNameToUse)
        }

        Log.e("Debin-2", sPackageNameToUse)
        //return "com.viasat.browser"

        if (sPackageNameToUse != VB_PACKAGE) {
            postToastMessage(context, "For better performance, please install viasat browser")
        }
        return sPackageNameToUse
    }

    /**
     * Used to check whether there is a specialized handler for a given intent.
     *
     * @param intent The intent to check with.
     * @return Whether there is a specialized handler for the given intent.
     */
    private fun hasSpecializedHandlerIntents(context: Context, intent: Intent): Boolean {
        try {
            val pm = context.packageManager
            val handlers = pm.queryIntentActivities(
                    intent,
                    PackageManager.GET_RESOLVED_FILTER)
            if (handlers == null || handlers.size == 0) {
                return false
            }
            for (resolveInfo in handlers) {
                val filter = resolveInfo.filter ?: continue
                if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue
                if (resolveInfo.activityInfo == null) continue
                return true
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Runtime exception while getting specialized handlers")
        }

        return false
    }
}
