package com.droidnova.notificationhistory.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.droidnova.notificationhistory.utils.CrashReporter
import kotlinx.coroutines.delay

fun isBatteryOptimizationIgnored(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

/** Live battery-exemption status plus the action that asks the user for it. */
@Stable
class BatteryOptimizationState internal constructor(
    private val ignored: State<Boolean>,
    val requestExemption: () -> Unit
) {
    val isIgnored: Boolean get() = ignored.value
}

/**
 * Re-reads the exemption whenever the user comes back from the system screen (activity result
 * and ON_RESUME), and again shortly after, because some OEM settings apply the change a moment
 * after returning. A card driven by [BatteryOptimizationState.isIgnored] therefore hides only
 * once the exemption is really granted.
 */
@Composable
fun rememberBatteryOptimizationState(): BatteryOptimizationState {
    val context = LocalContext.current
    val ignored = remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var recheckRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(recheckRequest) {
        for (delayMs in RECHECK_DELAYS_MS) {
            delay(delayMs)
            ignored.value = isBatteryOptimizationIgnored(context)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recheckRequest++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { recheckRequest++ }

    return remember(launcher) {
        BatteryOptimizationState(ignored) {
            val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
            val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            try {
                launcher.launch(requestIntent)
            } catch (_: ActivityNotFoundException) {
                runCatching { launcher.launch(settingsIntent) }.onFailure(CrashReporter::record)
            }
        }
    }
}

fun openNotificationAccessSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private val RECHECK_DELAYS_MS = longArrayOf(0L, 500L, 1_000L)
