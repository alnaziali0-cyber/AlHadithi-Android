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
        GovernorateInfo("baghdad", "بغداد", "Baghdad", 33.341, 44.401),
        GovernorateInfo("basra", "البصرة", "Basra", 30.509, 47.780),
        GovernorateInfo("najaf", "النجف", "Najaf", 32.026, 44.346),
        GovernorateInfo("karbala", "كربلاء", "Karbala", 32.616, 44.025),
        GovernorateInfo("babil", "بابل", "Babil", 32.464, 44.420),
        GovernorateInfo("wasit", "واسط", "Wasit", 32.513, 45.818),
        GovernorateInfo("qadisiyah", "القادسية", "Al-Qadisiyyah", 31.993, 44.926),
        GovernorateInfo("muthanna", "المثنى", "Al-Muthanna", 31.311, 45.281),
        GovernorateInfo("dhi_qar", "ذي قار", "Dhi Qar", 31.058, 46.257),
        GovernorateInfo("maysan", "ميسان", "Maysan", 31.836, 47.145),
        GovernorateInfo("diyala", "ديالى", "Diyala", 33.744, 44.643),
        GovernorateInfo("anbar", "الأنبار", "Al-Anbar", 33.425, 43.300),
        GovernorateInfo("saladin", "صلاح الدين", "Saladin", 34.607, 43.678),
        GovernorateInfo("nineveh", "نينوى", "Nineveh", 36.335, 43.119),
        GovernorateInfo("kirkuk", "كركوك", "Kirkuk", 35.468, 44.392),
        GovernorateInfo("erbil", "أربيل", "Erbil", 36.191, 44.009),
        GovernorateInfo("sulaymaniyah", "السليمانية", "Sulaymaniyah", 35.565, 45.433),
        GovernorateInfo("duhok", "دهوك", "Duhok", 36.866, 42.988)
    )

    fun byId(id: String): GovernorateInfo =
        all.firstOrNull { it.id == id } ?: all.first()

    fun nearestTo(lat: Double, lon: Double): GovernorateInfo =
        all.minByOrNull { QiblaGeoMath.distanceBetweenKm(lat, lon, it.latitude, it.longitude) }
            ?: all.first()
}
