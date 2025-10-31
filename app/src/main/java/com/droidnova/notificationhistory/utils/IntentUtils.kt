package com.droidnova.notificationhistory.utils


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.util.Locale
import androidx.core.net.toUri

object IntentUtils {
    //RateUs
    fun rateUs(context: Context) {
        val appPackageName = context.packageName
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=$appPackageName".toUri()
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: android.content.ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$appPackageName".toUri()
                )
            )
        }
    }

    fun shareApp(context: Context) {
        val appPackageName = context.packageName
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out this app: https://play.google.com/store/apps/details?id=$appPackageName"
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share app via"))
    }


    fun fetchAppVersion(context: Context): String {
        return try {
            val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                ).versionName
            }

            "$versionName"
        } catch (e: Exception) {
            "Version: Unknown"
        }
    }

    fun joinWhatsappCommunity(context: Context) {
        val communityLink = "https://chat.whatsapp.com/H8WJIiLFWXDEO6tSsEoRqR"
        try {
            context.packageManager?.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = communityLink.toUri() })
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("myTag", "whatsapp not installed")
            context.startActivity(Intent(Intent.ACTION_VIEW, communityLink.toUri()))
        }
    }

    fun joinInstagramCommunity(context: Context) {
        val communityLink = "https://www.instagram.com/droid_nova?igsh=MWdjMGtsZGNmMm45dg=="
        try {
            context.packageManager?.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = communityLink.toUri() })
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("myTag", "whatsapp not installed")
            context.startActivity(Intent(Intent.ACTION_VIEW, communityLink.toUri()))
        }
    }

    fun openBVRAppOnPlayStore(context: Context) {
        val packageName = "com.droidnova.backgroundcamera"
        try {
            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            intent.setPackage("com.android.vending")
            context.startActivity(intent)
            Log.e("myTag playstore","opened")
        } catch (e: ActivityNotFoundException) {
            context.let { Toast.makeText(it,"App Not Found On PlayStore",Toast.LENGTH_SHORT).show() }
            Log.e("myTag playstore","failed")

        }
    }

    fun openClipboardHistoryAppOnPlayStore(context: Context) {
        val packageName = "com.droidnova.clipboardhistory"
        try {
            val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
            intent.setPackage("com.android.vending")
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.let { Toast.makeText(it,"App Not Found On PlayStore",Toast.LENGTH_SHORT).show() }
        }
    }

    fun openDeveloperDashboardOnPlayStore(context: Context) {
        val dashBoardLink = "https://play.google.com/store/apps/dev?id=8766579115812433944"
        try {
            val intent = Intent(Intent.ACTION_VIEW, dashBoardLink.toUri())
            intent.setPackage("com.android.vending")
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.let { Toast.makeText(it,"App Not Found On PlayStore",Toast.LENGTH_SHORT).show() }
        }
    }

    fun sendFeedback(context: Context) {
        val email = Constants.SUPPORT_EMAIL

        val packageManager = context.packageManager
        val appName = context.applicationInfo.loadLabel(packageManager).toString()
        val versionName = try {
            packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }

        val deviceBrand = Build.BRAND.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val deviceModel = Build.MODEL
        val apiLevel = Build.VERSION.SDK_INT
        val androidVersion = Build.VERSION.RELEASE

        val subject = "App Feedback - $appName ($versionName)"
        val body = """Device: $deviceBrand $deviceModel \nAndroid Version: $androidVersion (API $apiLevel) \n \n--- Write your feedback below this line ---\n""".trimIndent()


        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri() // Only email apps will handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        // Optional: Force Gmail if installed
        val gmailPackage = "com.google.android.gm"
        if (isPackageInstalled(gmailPackage, context)) {
            intent.setPackage(gmailPackage)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun reportBugs(context: Context) {
        val email = "droidnova7@gmail.com"  // or Constants.SUPPORT_EMAIL if you prefer
        val packageManager = context.packageManager
        val appName = context.applicationInfo.loadLabel(packageManager).toString()
        val versionName = try {
            packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }

        val deviceBrand = Build.BRAND.replaceFirstChar { it.titlecase(Locale.ROOT) }
        val deviceModel = Build.MODEL
        val apiLevel = Build.VERSION.SDK_INT
        val androidVersion = Build.VERSION.RELEASE

        val subject = "Bug Report - $appName ($versionName)"
        val body = """
        Device: $deviceBrand $deviceModel
        Android Version: $androidVersion (API $apiLevel)
        
        --- Describe the issue below this line ---
        
    """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        // Optional: Force Gmail if installed
        val gmailPackage = "com.google.android.gm"
        if (isPackageInstalled(gmailPackage, context)) {
            intent.setPackage(gmailPackage)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }


    private fun isPackageInstalled(packageName: String, context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
