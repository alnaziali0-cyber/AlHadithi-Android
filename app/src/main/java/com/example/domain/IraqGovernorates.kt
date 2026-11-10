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
        GovernorateInfo("najaf", "النجف", "Najaf", 31.9963, 44.3142),
        GovernorateInfo("karbala", "كربلاء", "Karbala", 32.616, 44.0249),
        GovernorateInfo("babil", "بابل", "Babil", 32.4637, 44.4197),
        GovernorateInfo("wasit", "واسط", "Wasit", 32.5136, 45.8181),
        GovernorateInfo("qadisiyah", "القادسية", "Al-Qadisiyyah", 31.989, 44.925),
        GovernorateInfo("muthanna", "المثنى", "Al-Muthanna", 31.33, 45.281),
        GovernorateInfo("dhi_qar", "ذي قار", "Dhi Qar", 31.0539, 46.2594),
        GovernorateInfo("maysan", "ميسان", "Maysan", 31.8333, 47.15),
        GovernorateInfo("diyala", "ديالى", "Diyala", 33.75, 44.65),
        GovernorateInfo("anbar", "الأنبار", "Al-Anbar", 33.4258, 43.3081),
        GovernorateInfo("saladin", "صلاح الدين", "Saladin", 34.6111, 43.6786),
        GovernorateInfo("nineveh", "نينوى", "Nineveh", 36.335, 43.1189),
        GovernorateInfo("kirkuk", "كركوك", "Kirkuk", 35.4681, 44.3922),
        GovernorateInfo("erbil", "أربيل", "Erbil", 36.1911, 44.0092),
        GovernorateInfo("sulaymaniyah", "السليمانية", "Sulaymaniyah", 35.5556, 45.4351),
        GovernorateInfo("duhok", "دهوك", "Duhok", 36.8669, 42.9883),
        GovernorateInfo("halabja", "حلبجة", "Halabja", 35.18639, 45.97389)
    )

    fun byId(id: String): GovernorateInfo =
        all.firstOrNull { it.id == id } ?: all.first()

    fun nearestTo(lat: Double, lon: Double): GovernorateInfo =
        all.minByOrNull { QiblaGeoMath.distanceBetweenKm(lat, lon, it.latitude, it.longitude) }
            ?: all.first()
}

/**
 * مناطق ومدن العراق بأحداثياتها.
 *
 * البيانات مأخوذة من قائمة موثّقة تُراجَع مقابل مصادر الخريطة، وتُستخدم
 * لحساب مواقيت الصلاة بدقة أعلى من مركز المحافظة alone.
 */
data class DistrictInfo(
    val id: String,
    val arabicName: String,
    val latitude: Double,
    val longitude: Double
)

object IraqDistricts {
    private fun d(id: String, name: String, lat: Double, lon: Double) = DistrictInfo(id, name, lat, lon)

    /** كل المناطق مجمّعة حسب معرّف المحافظة. */
    val byGovernorate: Map<String, List<DistrictInfo>> = mapOf(
        "baghdad" to listOf(
            d("baghdad-1", "بغداد - الكاظمية", 33.3936, 44.4623),
            d("baghdad-2", "الأعظمية", 33.3365, 44.3636),
            d("baghdad-3", "المنصور", 33.3119, 44.2718),
            d("baghdad-4", "الكرخ", 33.3387, 44.4648),
            d("baghdad-5", "زيونة", 33.3436, 44.4618),
            d("baghdad-6", "الدورة", 33.3068, 44.4394),
            d("baghdad-7", "الشعلة", 33.3074, 44.2717),
            d("baghdad-8", "البياع", 33.2786, 44.3933),
            d("baghdad-9", "الغزالية", 33.3177, 44.232),
            d("baghdad-10", "أبو غريب", 33.3094, 44.0247),
        ),
        "basra" to listOf(
            d("basra-1", "البصرة", 30.5085, 47.7804),
            d("basra-2", "المعقل", 30.5567, 47.6972),
            d("basra-3", "أبو الخصيب", 30.5147, 47.4617),
            d("basra-4", "الزبير", 30.3895, 47.5936),
            d("basra-5", "القرنة", 30.9894, 47.4272),
            d("basra-6", "شط العرب", 30.545, 47.795),
            d("basra-7", "تنومة", 30.5261, 47.6897),
        ),
        "nineveh" to listOf(
            d("nineveh-1", "الموصل", 36.335, 43.1189),
            d("nineveh-2", "تلعفر", 36.1733, 43.6789),
            d("nineveh-3", "سنجار", 36.3758, 42.9872),
            d("nineveh-4", "بعشيقة", 36.4067, 43.1303),
            d("nineveh-5", "الحمدانية", 36.8167, 42.3833),
            d("nineveh-6", "عقرة", 36.51, 42.86),
        ),
        "erbil" to listOf(
            d("erbil-1", "أربيل", 36.1911, 44.0092),
            d("erbil-2", "عنكاوا", 36.2114, 44.0272),
            d("erbil-3", "شقلاوة", 36.235, 43.635),
            d("erbil-4", "سوران", 36.54, 43.54),
            d("erbil-5", "كويسنجق", 36.48, 44.23),
            d("erbil-6", "شندوخا", 36.7556, 43.6944),
        ),
        "sulaymaniyah" to listOf(
            d("sulaymaniyah-1", "السليمانية", 35.5556, 45.4351),
            d("sulaymaniyah-2", "رانية", 35.2931, 45.8186),
            d("sulaymaniyah-3", "دوكان", 35.1128, 45.9917),
            d("sulaymaniyah-4", "جمجمال", 35.9333, 45.4667),
            d("sulaymaniyah-5", "كفري", 35.13, 45.66),
            d("sulaymaniyah-6", "چوارباخ", 35.0489, 45.8256),
        ),
        "duhok" to listOf(
            d("duhok-1", "دهوك", 36.8669, 42.9883),
            d("duhok-2", "زاخو", 36.76, 42.98),
            d("duhok-3", "سميلين", 36.81, 43.15),
            d("duhok-4", "العمادية", 37.09, 43.68),
            d("duhok-5", "شندوخا - دهوك", 36.735, 43.12),
            d("duhok-6", "مالطا", 36.71, 42.95),
        ),
        "kirkuk" to listOf(
            d("kirkuk-1", "كركوك", 35.4681, 44.3922),
            d("kirkuk-2", "الحويجة", 35.1442, 43.8797),
            d("kirkuk-3", "الدبس", 35.0333, 43.9833),
            d("kirkuk-4", "شناش", 35.2167, 44.2167),
            d("kirkuk-5", "داق", 35.35, 44.05),
        ),
        "diyala" to listOf(
            d("diyala-1", "بعقوبة", 33.75, 44.65),
            d("diyala-2", "المقدادية", 33.6833, 44.5),
            d("diyala-3", "خانقين", 34.2, 45.1667),
            d("diyala-4", "بلدروز", 33.87, 44.75),
            d("diyala-5", "الخالص", 33.6333, 44.3167),
            d("diyala-6", "جرف الصخر", 33.51, 44.2),
        ),
        "anbar" to listOf(
            d("anbar-1", "الرمادي", 33.4258, 43.3081),
            d("anbar-2", "الفلوجة", 33.3556, 43.5792),
            d("anbar-3", "هيت", 33.6389, 42.825),
            d("anbar-4", "حديثة", 34.1372, 42.3797),
            d("anbar-5", "القائم", 34.3667, 41.1833),
            d("anbar-6", "الخالدية", 33.3794, 43.4886),
        ),
        "saladin" to listOf(
            d("saladin-1", "سامراء", 34.1959, 43.8741),
            d("saladin-2", "بلد", 34.0167, 43.4833),
            d("saladin-3", "الدجيل", 33.9833, 44.0833),
            d("saladin-4", "تكريت", 34.6111, 43.6786),
            d("saladin-5", "بيجي", 34.35, 43.4833),
            d("saladin-6", "الشرقاط", 34.7333, 43.7),
        ),
        "karbala" to listOf(
            d("karbala-1", "كربلاء", 32.616, 44.0249),
            d("karbala-2", "الهندية", 32.1939, 44.0292),
            d("karbala-3", "عين التمر", 32.52, 44.26),
            d("karbala-4", "حي الحسين", 32.61, 44.015),
        ),
        "najaf" to listOf(
            d("najaf-1", "النجف", 31.9963, 44.3142),
            d("najaf-2", "الكوفة", 32.0297, 44.3386),
            d("najaf-3", "المشخاب", 31.9772, 44.2417),
            d("najaf-4", "المناذرة", 31.8403, 44.4178),
            d("najaf-5", "حي السلام", 31.995, 44.32),
        ),
        "babil" to listOf(
            d("babil-1", "الحلة", 32.4637, 44.4197),
            d("babil-2", "المحاويل", 32.7667, 44.45),
            d("babil-3", "المسيب", 32.525, 44.3167),
            d("babil-4", "جرف الصخر - بابل", 32.54, 44.35),
        ),
        "wasit" to listOf(
            d("wasit-1", "الكوت", 32.5136, 45.8181),
            d("wasit-2", "النعمانية", 32.5236, 45.3986),
            d("wasit-3", "الصويرة", 32.01, 45.97),
            d("wasit-4", "بدرة", 32.4333, 45.9333),
            d("wasit-5", "الجي", 32.64, 45.5),
        ),
        "qadisiyah" to listOf(
            d("qadisiyah-1", "الديوانية", 31.989, 44.925),
            d("qadisiyah-2", "الشامية", 31.9267, 44.8167),
            d("qadisiyah-3", "عفك", 32.2139, 45.0778),
            d("qadisiyah-4", "الحمزة", 31.9333, 45.0667),
            d("qadisiyah-5", "جمال", 31.8, 44.95),
        ),
        "muthanna" to listOf(
            d("muthanna-1", "السامراء - المثنى", 31.33, 45.281),
            d("muthanna-2", "الرميثة", 31.4314, 45.2917),
            d("muthanna-3", "الخضر", 31.53, 45.15),
            d("muthanna-4", "السلمان", 30.68, 45.55),
            d("muthanna-5", "سوق الشيوخ", 30.45, 45.75),
        ),
        "dhi_qar" to listOf(
            d("dhi_qar-1", "الناصرية", 31.0539, 46.2594),
            d("dhi_qar-2", "الشطرة", 30.9614, 46.1625),
            d("dhi_qar-3", "الرفاعي", 31.4333, 46.0833),
            d("dhi_qar-4", "الجبايش", 31.15, 46.7),
            d("dhi_qar-5", "قلعة سكر", 31.1167, 47.1333),
        ),
        "maysan" to listOf(
            d("maysan-1", "العمارة", 31.8333, 47.15),
            d("maysan-2", "المجر الكبير", 31.19, 47.3),
            d("maysan-3", "الكحلاء", 31.5, 47.2),
            d("maysan-4", "علي الغربي", 31.55, 47.5),
            d("maysan-5", "قلعة صالح", 31.2, 47.15),
        ),
        "halabja" to listOf(
            d("halabja-1", "حلبجة", 35.18639, 45.97389),
        ),
    )

    fun of(governorateId: String): List<DistrictInfo> = byGovernorate[governorateId].orEmpty()

    fun byId(districtId: String): DistrictInfo? =
        byGovernorate.values.flatten().firstOrNull { it.id == districtId }

    /** أقرب منطقة إلى إحداثيات معيّنة (تُستخدم بعد تحديد الموقع). */
    fun nearestTo(latitude: Double, longitude: Double): DistrictInfo? =
        byGovernorate.values.flatten().minByOrNull {
            val dLat = it.latitude - latitude
            val dLon = it.longitude - longitude
            dLat * dLat + dLon * dLon
        }
}
