package com.example.domain

/** Verified public information published by Radio Al-Rawda Al-Hussainiya. */
data class RadioFrequency(
    val governorateArabic: String,
    val frequencyMhz: Double
)

object ImamHussainRadioCatalog {
    const val officialHomeUrl: String = "https://imamhussain-fm.com/"
    const val officialProgramsUrl: String = "https://imamhussain-fm.com/programs"

    /**
     * FM frequencies transcribed from the station's official website.
     * This list is informational and does not represent live stream URLs.
     */
    val frequencies: List<RadioFrequency> = listOf(
        RadioFrequency("بغداد", 93.5),
        RadioFrequency("كربلاء", 88.3),
        RadioFrequency("بابل", 107.5),
        RadioFrequency("النجف", 106.3),
        RadioFrequency("واسط", 92.9),
        RadioFrequency("الديوانية", 93.3),
        RadioFrequency("المثنى", 90.2),
        RadioFrequency("ذي قار", 103.9),
        RadioFrequency("ميسان", 90.6),
        RadioFrequency("البصرة", 103.9)
    )
}
