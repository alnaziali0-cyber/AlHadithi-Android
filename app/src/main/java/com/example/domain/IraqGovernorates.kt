package com.example.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GovernorateInfo(
    val id: String,
    val arabicName: String,
    val englishName: String,
    val latitude: Double,
    val longitude: Double
) {
    val qiblaBearingDegrees: Int
        get() = QiblaGeoMath.calculateQiblaBearing(latitude, longitude).roundToInt()

    val distanceToKaabaKm: Int
        get() = QiblaGeoMath.calculateDistanceToKaabaKm(latitude, longitude).roundToInt()
}

object QiblaGeoMath {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0

    fun calculateQiblaBearing(latDeg: Double, lonDeg: Double): Double {
        val phi1 = Math.toRadians(latDeg)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLon = Math.toRadians(KAABA_LON - lonDeg)

        val y = sin(deltaLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLon)
        val theta = Math.toDegrees(atan2(y, x))
        return (theta + 360.0) % 360.0
    }

    fun calculateDistanceToKaabaKm(latDeg: Double, lonDeg: Double): Double {
        val dLat = Math.toRadians(KAABA_LAT - latDeg)
        val dLon = Math.toRadians(KAABA_LON - lonDeg)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(latDeg)) * cos(Math.toRadians(KAABA_LAT)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    fun distanceBetweenKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
}

object IraqGovernorates {
    val all: List<GovernorateInfo> = listOf(
        GovernorateInfo("baghdad", "بغداد", "Baghdad", 33.3152, 44.3661),
        GovernorateInfo("basra", "البصرة", "Basra", 30.5085, 47.7804),
        GovernorateInfo("najaf", "النجف", "Najaf", 31.9959, 44.3146),
        GovernorateInfo("karbala", "كربلاء", "Karbala", 32.6160, 44.0249),
        GovernorateInfo("babil", "بابل", "Babil", 32.4637, 44.4196),
        GovernorateInfo("wasit", "واسط", "Wasit", 32.5128, 45.8182),
        GovernorateInfo("qadisiyah", "القادسية", "Al-Qadisiyyah", 31.9889, 44.9255),
        GovernorateInfo("muthanna", "المثنى", "Al-Muthanna", 31.3320, 45.2944),
        GovernorateInfo("dhi_qar", "ذي قار", "Dhi Qar", 31.0439, 46.2573),
        GovernorateInfo("maysan", "ميسان", "Maysan", 31.8359, 47.1448),
        GovernorateInfo("diyala", "ديالى", "Diyala", 33.7485, 44.6433),
        GovernorateInfo("anbar", "الأنبار", "Al-Anbar", 33.4258, 43.2994),
        GovernorateInfo("saladin", "صلاح الدين", "Saladin", 34.6111, 43.6782),
        GovernorateInfo("nineveh", "نينوى", "Nineveh", 36.3489, 43.1577),
        GovernorateInfo("kirkuk", "كركوك", "Kirkuk", 35.4681, 44.3922),
        GovernorateInfo("erbil", "أربيل", "Erbil", 36.1901, 44.0091),
        GovernorateInfo("sulaymaniyah", "السليمانية", "Sulaymaniyah", 35.5613, 45.4304),
        GovernorateInfo("duhok", "دهوك", "Duhok", 36.8671, 42.9884)
    )

    fun byId(id: String): GovernorateInfo =
        all.firstOrNull { it.id == id } ?: all.first()

    fun nearestTo(lat: Double, lon: Double): GovernorateInfo =
        all.minByOrNull { QiblaGeoMath.distanceBetweenKm(lat, lon, it.latitude, it.longitude) }
            ?: all.first()
}
