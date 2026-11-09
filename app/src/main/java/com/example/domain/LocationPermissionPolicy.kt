package com.example.domain

/** Decisions for the single foreground location-permission prompt used by the whole app. */
enum class LocationPermissionAction {
    USE_GRANTED_PERMISSION,
    REQUEST_ONCE,
    OPEN_APP_SETTINGS
}

object LocationPermissionPolicy {
    const val MAX_ACCEPTED_ACCURACY_METERS = 100f
    const val MAX_FIX_AGE_MILLIS = 120_000L

    fun nextAction(
        hasFinePermission: Boolean,
        hasCoarsePermission: Boolean,
        requestAlreadyShown: Boolean,
        shouldShowSystemRationale: Boolean
    ): LocationPermissionAction = when {
        hasFinePermission -> LocationPermissionAction.USE_GRANTED_PERMISSION
        hasCoarsePermission || requestAlreadyShown || shouldShowSystemRationale ->
            LocationPermissionAction.OPEN_APP_SETTINGS
        else -> LocationPermissionAction.REQUEST_ONCE
    }

    fun isFreshFix(fixTimeMillis: Long, nowMillis: Long): Boolean {
        val age = nowMillis - fixTimeMillis
        return age in 0..MAX_FIX_AGE_MILLIS
    }

    fun isAccurateFix(accuracyMeters: Float): Boolean =
        accuracyMeters.isFinite() && accuracyMeters > 0f &&
            accuracyMeters <= MAX_ACCEPTED_ACCURACY_METERS
}
