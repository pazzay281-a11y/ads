package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AdIdResult(
    val id: String = "",
    val isLimitAdTrackingEnabled: Boolean = false,
    val isZeroedOut: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object AdResetHelper {
    private const val TAG = "AdResetHelper"

    /**
     * Attempts to open the Google Ads / Ad Privacy Settings screen directly.
     * Uses fallbacks across various OEM skins and Android OS versions.
     */
    fun openGoogleAdsSettings(context: Context): Boolean {
        vibrateDevice(context, 35)

        val intentsToTry = listOf(
            // Primary direct Play Services Ads Intent
            Intent("com.google.android.gms.settings.ADS_PRIVACY").apply {
                setPackage("com.google.android.gms")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Without package constraint
            Intent("com.google.android.gms.settings.ADS_PRIVACY").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Secondary Ads intent
            Intent("com.google.android.gms.settings.PRIVACY_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Generic Google Ads Settings
            Intent("com.google.android.gms.settings.ADS_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Android 12+ Privacy Dashboard / Ads
            Intent("android.settings.PRIVACY_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Google Settings Activity in Google Play Services
            Intent().apply {
                setClassName("com.google.android.gms", "com.google.android.gms.app.settings.GoogleSettingsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Google Play Services App Info
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:com.google.android.gms")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            // Device Main Settings fallback
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        for (intent in intentsToTry) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Successfully started intent: ${intent.action}")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed intent ${intent.action}: ${e.message}")
            }
        }

        // Final attempt without resolve check (some OEM OSs hide Play Services activities from resolveActivity)
        try {
            val directIntent = Intent("com.google.android.gms.settings.ADS_PRIVACY").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(directIntent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "All direct ads intents failed: ${e.message}")
        }

        // Absolute fallback to Settings
        return try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            Toast.makeText(context, "Opened Settings. Navigate to Google -> Ads to reset.", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Settings: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens Android Privacy Sandbox / Ad Topics settings if available
     */
    fun openPrivacySandboxSettings(context: Context): Boolean {
        vibrateDevice(context, 30)
        val intents = listOf(
            Intent("android.adservices.action.MEASUREMENT_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent("android.adservices.action.TOPICS_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent("android.settings.PRIVACY_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }
        return openGoogleAdsSettings(context)
    }

    /**
     * Opens Google Play Services info page
     */
    fun openPlayServicesDetails(context: Context) {
        vibrateDevice(context, 30)
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:com.google.android.gms")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Inspects Google Advertising ID using Play Services Client
     */
    suspend fun getAdvertisingId(context: Context): AdIdResult = withContext(Dispatchers.IO) {
        try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            val id = info.id ?: "Not Available"
            val isZeroed = id == "00000000-0000-0000-0000-000000000000"
            val limitTracking = info.isLimitAdTrackingEnabled

            AdIdResult(
                id = id,
                isLimitAdTrackingEnabled = limitTracking,
                isZeroedOut = isZeroed,
                success = true,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error querying AdvertisingIdClient", e)
            AdIdResult(
                id = "Unavailable",
                isLimitAdTrackingEnabled = false,
                isZeroedOut = false,
                success = false,
                errorMessage = e.localizedMessage ?: "Failed to read Advertising ID"
            )
        }
    }

    /**
     * Checks if SYSTEM_ALERT_WINDOW (Draw over other apps) is granted
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Navigates user directly to Overlay Permission screen for this app
     */
    fun requestOverlayPermission(context: Context) {
        vibrateDevice(context, 40)
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay settings", e)
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    /**
     * Copies string to clipboard with feedback
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Google Advertising ID") {
        vibrateDevice(context, 30)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Haptic feedback helper
     */
    fun vibrateDevice(context: Context, milliseconds: Long = 30) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(milliseconds)
            }
        } catch (_: Exception) {
            // Safe ignore if vibration is not allowed/supported
        }
    }
}
