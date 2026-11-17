"""Jafari solar-time calculations.

Default preset is the documented Jafari (Qom) angle set: Fajr 16°, Maghrib 4°,
Isha 14°. This is a calculation preset, not a religious ruling. Local marja'
requirements and official timetables can differ; clients must expose the method
and parameters to the user.
"""
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta
from math import acos, atan, cos, degrees, radians, sin, tan
from zoneinfo import ZoneInfo


@dataclass(frozen=True)
class PrayerMethod:
    id: str
    label_ar: str
    label_en: str
    fajr_angle: float
    maghrib_angle: float
    isha_angle: float
    source_url: str


METHODS = {
    "jafari_qom": PrayerMethod(
        id="jafari_qom", label_ar="الجعفرية (قم)", label_en="Jafari (Qom)",
        fajr_angle=16.0, maghrib_angle=4.0, isha_angle=14.0,
        source_url="https://theshia.org/ar/api/methods/",
    ),
    "tehran": PrayerMethod(
        id="tehran", label_ar="جامعة طهران", label_en="University of Tehran",
        fajr_angle=17.7, maghrib_angle=4.5, isha_angle=14.0,
        source_url="https://theshia.org/ar/api/methods/",
    ),
}


def _solar_terms(day: date) -> tuple[float, float]:
    # NOAA fractional-year approximation evaluated at local solar noon.
    gamma = 2.0 * 3.141592653589793 / 365.0 * (day.timetuple().tm_yday - 1)
    eq_time = 229.18 * (
        0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma)
        - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
    )
    decl = (
        0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma)
        - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma)
        - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
    )
    return eq_time, decl


def _event_minutes(day: date, latitude: float, longitude: float, utc_offset_minutes: int,
                   altitude_degrees: float, morning: bool) -> float:
    eq_time, decl = _solar_terms(day)
    lat_r, alt_r = radians(latitude), radians(altitude_degrees)
    denominator = cos(lat_r) * cos(decl)
    if abs(denominator) < 1e-12:
        raise ValueError("Sun angle is undefined for these coordinates")
    cos_hour_angle = (sin(alt_r) - sin(lat_r) * sin(decl)) / denominator
    if cos_hour_angle < -1.0 or cos_hour_angle > 1.0:
        raise ValueError("The requested solar event does not occur at this latitude/date")
    hour_angle = degrees(acos(max(-1.0, min(1.0, cos_hour_angle))))
    solar_noon = 720.0 - 4.0 * longitude - eq_time + utc_offset_minutes
    return solar_noon - 4.0 * hour_angle if morning else solar_noon + 4.0 * hour_angle


def _as_local_datetime(day: date, minute_of_day: float, zone: ZoneInfo) -> datetime:
    # Round once to the nearest minute; carry across day boundaries naturally.
    total_minutes = int(round(minute_of_day))
    return datetime.combine(day, time.min, tzinfo=zone) + timedelta(minutes=total_minutes)


def calculate_prayer_times(day: date, latitude: float, longitude: float,
                            method_id: str = "jafari_qom", timezone_name: str = "Asia/Baghdad") -> dict:
    if not (-90 <= latitude <= 90 and -180 <= longitude <= 180):
        raise ValueError("Latitude or longitude is outside the valid range")
    method = METHODS.get(method_id)
    if method is None:
        raise ValueError("Unsupported prayer calculation method")
    zone = ZoneInfo(timezone_name)
    local_noon = datetime.combine(day, time(12, 0), tzinfo=zone)
    offset = int(local_noon.utcoffset().total_seconds() // 60)

    fajr_min = _event_minutes(day, latitude, longitude, offset, -method.fajr_angle, True)
    sunrise_min = _event_minutes(day, latitude, longitude, offset, -0.833, True)
    equation_of_time, declination = _solar_terms(day)
    dhuhr_min = 720.0 - 4.0 * longitude - equation_of_time + offset
    # Standard Asr convention: object shadow = noon shadow + 1 x object height.
    # PrayTimes documents shadow factor 1 as the Standard method used by Jafari.
    latitude_radians = radians(latitude)
    asr_shadow_factor = 1.0
    asr_shadow_ratio = asr_shadow_factor + tan(abs(latitude_radians - declination))
    asr_altitude = degrees(atan(1.0 / asr_shadow_ratio))
    asr_min = _event_minutes(day, latitude, longitude, offset, asr_altitude, False)
    sunset_min = _event_minutes(day, latitude, longitude, offset, -0.833, False)
    maghrib_min = _event_minutes(day, latitude, longitude, offset, -method.maghrib_angle, False)
    isha_min = _event_minutes(day, latitude, longitude, offset, -method.isha_angle, False)

    next_day = day + timedelta(days=1)
    next_noon = datetime.combine(next_day, time(12, 0), tzinfo=zone)
    next_offset = int(next_noon.utcoffset().total_seconds() // 60)
    next_fajr_min = _event_minutes(next_day, latitude, longitude, next_offset, -method.fajr_angle, True)
    sunset_dt = _as_local_datetime(day, sunset_min, zone)
    next_fajr_dt = _as_local_datetime(next_day, next_fajr_min, zone)
    islamic_midnight = sunset_dt + (next_fajr_dt - sunset_dt) / 2
    maghrib_dt = _as_local_datetime(day, maghrib_min, zone)
    islamic_midnight_latest_by = maghrib_dt + (next_fajr_dt - maghrib_dt) / 2

    events = {
        "fajr": _as_local_datetime(day, fajr_min, zone),
        "sunrise": _as_local_datetime(day, sunrise_min, zone),
        "dhuhr": _as_local_datetime(day, dhuhr_min, zone),
        "asr": _as_local_datetime(day, asr_min, zone),
        "sunset": sunset_dt,
        "maghrib": _as_local_datetime(day, maghrib_min, zone),
        "isha": _as_local_datetime(day, isha_min, zone),
        "islamic_midnight": islamic_midnight,
        "islamic_midnight_latest_by": islamic_midnight_latest_by,
    }
    return {
        "date": day.isoformat(),
        "timezone": timezone_name,
        "method": {
            "id": method.id,
            "label_ar": method.label_ar,
            "label_en": method.label_en,
            "parameters": {
                "fajr_sun_depression_degrees": method.fajr_angle,
                "maghrib_sun_depression_degrees": method.maghrib_angle,
                "isha_sun_depression_degrees": method.isha_angle,
            },
            "reference": method.source_url,
        },
        "times": {key: value.isoformat() for key, value in events.items()},
        "display_times": {key: value.strftime("%H:%M") for key, value in events.items()},
        "definitions": {
            "sunrise_sunset": "Apparent solar center altitude -0.833 degrees (includes conventional refraction and solar radius).",
            "asr": "Standard shadow-factor calculation: the object shadow equals the noon shadow plus one object height (factor 1); PrayTimes documents Standard as the Jafari convention. Confirm the selected convention against the local marja timetable before relying on it.",
            "maghrib": f"Sun altitude reaches -{method.maghrib_angle:g} degrees; this is a calculation convention and may differ from local religious timetables.",
            "isha": f"Sun altitude reaches -{method.isha_angle:g} degrees.",
            "islamic_midnight": "Midpoint between apparent sunset and the following day's Fajr (the calculation convention documented by the selected source).",
            "islamic_midnight_latest_by": "Midpoint between calculated Maghrib and the following day's Fajr; shown separately because published conventions differ.",
        },
        "coordinate_quality": "provided_coordinate",
    }
