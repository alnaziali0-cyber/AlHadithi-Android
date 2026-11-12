package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.domain.LocationPermissionAction
import com.example.domain.LocationPermissionPolicy
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/** A single foreground location fix. No continuous or background tracking is performed. */
data class PreciseDeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAtMillis: Long
)

object PreciseDeviceLocationAccess {
    private const val PREFS_NAME = "alhadithi_location_permission_v1"
    private const val KEY_REQUEST_SHOWN = "precise_location_prompt_shown"

    fun hasPrecisePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun hasApproximatePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun hasShownPermissionPrompt(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REQUEST_SHOWN, false)

    fun markPermissionPromptShown(context: Context) {
        // Persist before opening Android UI so recreation cannot show a second prompt.
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REQUEST_SHOWN, true)
            .commit()
    }

    fun openApplicationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun isDeviceLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                manager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        }.getOrDefault(false)
    }

    fun openDeviceLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /**
     * Uses a fresh high-accuracy FLP fix and rejects stale or coarse-quality results.
     * The caller receives coordinates only after fine permission and the accuracy gate pass.
     */
    @SuppressLint("MissingPermission")
    fun requestFreshPreciseFix(
        context: Context,
        onResult: (Result<PreciseDeviceLocation>) -> Unit
    ) {
        if (!hasPrecisePermission(context)) {
            onResult(Result.failure(SecurityException("Precise location permission is not granted")))
            return
        }
        if (!isDeviceLocationEnabled(context)) {
            onResult(Result.failure(IllegalStateException("خدمات الموقع في الهاتف متوقفة")))
            return
        }

        val cancellation = CancellationTokenSource()
        try {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        onResult(Result.failure(IllegalStateException("لم يصل تثبيت GPS حديث")))
                        return@addOnSuccessListener
                    }

                    val now = System.currentTimeMillis()
                    if (!LocationPermissionPolicy.isFreshFix(location.time, now)) {
                        onResult(Result.failure(IllegalStateException("نتيجة الموقع قديمة؛ نحتاج قراءة جديدة")))
                        return@addOnSuccessListener
                    }
                    if (!location.hasAccuracy() ||
                        !LocationPermissionPolicy.isAccurateFix(location.accuracy)
                    ) {
                        onResult(Result.failure(IllegalStateException("دقة GPS الحالية أضعف من 100 متر")))
                        return@addOnSuccessListener
                    }
                    if (!location.latitude.isFinite() || location.latitude !in -90.0..90.0 ||
                        !location.longitude.isFinite() || location.longitude !in -180.0..180.0
                    ) {
                        onResult(Result.failure(IllegalStateException("إحداثيات الموقع غير صالحة")))
                        return@addOnSuccessListener
                    }

                    onResult(
                        Result.success(
                            PreciseDeviceLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracyMeters = location.accuracy,
                                capturedAtMillis = location.time
                            )
                        )
                    )
                }
                .addOnFailureListener { error: Exception -> onResult(Result.failure(error)) }
        } catch (error: SecurityException) {
            onResult(Result.failure(error))
        } catch (error: Exception) {
            onResult(Result.failure(error))
        }
    }
}

/**
 * Shared Compose permission flow. Android receives FINE + COARSE in ONE request so Android 12+
 * can show its Precise/Approximate selector. A denied request is not automatically repeated;
 * the next explicit tap opens this app's permission settings instead.
 */
@Composable
fun rememberPreciseDeviceLocationAction(
    onLocation: (PreciseDeviceLocation) -> Unit,
    onStatus: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val latestOnLocation = rememberUpdatedState(onLocation)
    val latestOnStatus = rememberUpdatedState(onStatus)

    fun acquireFreshFix() {
        if (!PreciseDeviceLocationAccess.hasPrecisePermission(context)) {
            latestOnStatus.value("الموقع الدقيق غير مفعّل. افتح إعدادات أذونات التطبيق واختر الموقع الدقيق.")
            return
        }
        if (!PreciseDeviceLocationAccess.isDeviceLocationEnabled(context)) {
            latestOnStatus.value("خدمة الموقع في الهاتف متوقفة؛ فعّلها ثم أعد المحاولة.")
            PreciseDeviceLocationAccess.openDeviceLocationSettings(context)
            return
        }
        latestOnStatus.value("جارٍ تحديد موقعك بدقة عالية…")
        PreciseDeviceLocationAccess.requestFreshPreciseFix(context) { result ->
            result.onSuccess { fix ->
                latestOnLocation.value(fix)
                latestOnStatus.value("تم تحديد الموقع بدقة ±${fix.accuracyMeters.toInt()} متر. لا يتتبع التطبيق موقعك في الخلفية.")
            }.onFailure { error ->
                latestOnStatus.value(
                    when (error.message) {
                        "خدمات الموقع في الهاتف متوقفة" -> "خدمة الموقع متوقفة؛ فعّل GPS ثم أعد المحاولة."
                        "لم يصل تثبيت GPS حديث" -> "لم يصل تثبيت GPS حديث. جرّب قرب نافذة أو في مكان مفتوح."
                        "نتيجة الموقع قديمة؛ نحتاج قراءة جديدة" -> "الموقع المحفوظ قديم؛ أعد المحاولة للحصول على قراءة حديثة."
                        "دقة GPS الحالية أضعف من 100 متر" -> "الإشارة الحالية غير دقيقة بما يكفي. انتقل إلى مكان مفتوح وحاول مجدداً."
                        else -> "تعذر تحديد الموقع بدقة. تحقق من GPS وأذونات التطبيق."
                    }
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result: Map<String, Boolean> ->
        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            PreciseDeviceLocationAccess.hasPrecisePermission(context)
        val coarseGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            PreciseDeviceLocationAccess.hasApproximatePermission(context)

        when {
            fineGranted -> acquireFreshFix()
            coarseGranted -> latestOnStatus.value(
                "تم اختيار الموقع التقريبي. لتحسين الدقة افتح إعدادات التطبيق وفعّل خيار الموقع الدقيق."
            )
            else -> latestOnStatus.value(
                "لم يُمنح إذن الموقع. يمكنك استخدام مركز المحافظة، ولن نكرر نافذة الإذن تلقائياً."
            )
        }
    }

    return remember(context, permissionLauncher) {
        {
            val activity = context.findActivity()
            val rationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } ?: false

            when (
                LocationPermissionPolicy.nextAction(
                    hasFinePermission = PreciseDeviceLocationAccess.hasPrecisePermission(context),
                    hasCoarsePermission = PreciseDeviceLocationAccess.hasApproximatePermission(context),
                    requestAlreadyShown = PreciseDeviceLocationAccess.hasShownPermissionPrompt(context),
                    shouldShowSystemRationale = rationale
                )
            ) {
                LocationPermissionAction.USE_GRANTED_PERMISSION -> acquireFreshFix()
                LocationPermissionAction.REQUEST_ONCE -> {
                    markPermissionPromptAndLaunch(context, permissionLauncher)
                }
                LocationPermissionAction.OPEN_APP_SETTINGS -> {
                    latestOnStatus.value(
                        "سبق أن ظهر طلب إذن الموقع. لتجنب تكراره افتح إعدادات التطبيق واختر الموقع الدقيق أثناء الاستخدام."
                    )
                    PreciseDeviceLocationAccess.openApplicationSettings(context)
                }
            }
        }
    }
}

private fun markPermissionPromptAndLaunch(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    PreciseDeviceLocationAccess.markPermissionPromptShown(context)
    // Request both permissions in a single dialog as required on Android 12+.
    launcher.launch(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
