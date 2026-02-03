package com.droidnova.notificationhistory.utils.about_utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import com.droidnova.notificationhistory.R
import java.util.Locale

object IntentUtil {

    fun openRateUs(context: Context) {
        openPlayStore(context, context.packageName)
    }

    fun shareApp(context: Context) {
        val appPackageName = context.packageName
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.share_app_text, appPackageName)
            )
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_app_chooser)))
    }

    fun openInstagram(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, AppConstants.INSTAGRAM_COMMUNITY_URL.toUri())
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, AppConstants.WHATSAPP_COMMUNITY_GROUP_URL.toUri())
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSupportMail(context: Context, isBug: Boolean) {
        val pm = context.packageManager
        val appName = context.applicationInfo.loadLabel(pm).toString()
        val versionName = try {
            pm.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            context.getString(R.string.unknown)
        }

        val deviceBrand = Build.BRAND.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val deviceModel = Build.MODEL
        val apiLevel = Build.VERSION.SDK_INT
        val androidVersion = Build.VERSION.RELEASE

        val subject = if (isBug) {
            context.getString(R.string.support_subject_bug, appName, versionName)
        } else {
            context.getString(R.string.support_subject_feedback, appName, versionName)
        }

        val body = context.getString(
            R.string.support_email_body,
            deviceBrand,
            deviceModel,
            androidVersion,
            apiLevel,
            if (isBug) context.getString(R.string.support_header_bug) else context.getString(R.string.support_header_feedback)
        )

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConstants.SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.no_email_app_found), Toast.LENGTH_SHORT).show()
        }
    }

    fun fetchAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: context.getString(R.string.version_unknown)
        } catch (e: Exception) {
            context.getString(R.string.version_unknown)
        }
    }

    fun openPlayStore(context: Context, packageName: String) {
        val uri = "market://details?id=$packageName".toUri()
        val webUri = "https://play.google.com/store/apps/details?id=$packageName".toUri()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun openDeveloperPlayConsole(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, AppConstants.PLAY_CONSOLE_URL.toUri())
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
        }
    }

    fun openApp(context: Context, packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            Toast.makeText(context, "Unable to open this app", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, chooserTitle: String, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }
}
