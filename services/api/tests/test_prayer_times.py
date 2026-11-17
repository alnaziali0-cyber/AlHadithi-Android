from datetime import date
from app.services.prayer_times import calculate_prayer_times


def test_babil_jafari_times_are_ordered_and_local():
    result = calculate_prayer_times(date(2026, 11, 13), 32.48, 44.43, "jafari_qom")
    times = result["display_times"]
    assert result["timezone"] == "Asia/Baghdad"
    assert times["fajr"] < times["sunrise"] < times["dhuhr"] < times["asr"] < times["sunset"] < times["maghrib"] < times["isha"]
    assert result["method"]["parameters"]["fajr_sun_depression_degrees"] == 16.0
    assert result["method"]["parameters"]["maghrib_sun_depression_degrees"] == 4.0
    assert result["method"]["parameters"]["isha_sun_depression_degrees"] == 14.0
    assert times["islamic_midnight"] > times["maghrib"]


def test_invalid_method_is_rejected():
    try:
        calculate_prayer_times(date(2026, 11, 13), 32.48, 44.43, "unknown")
    except ValueError as exc:
        assert "Unsupported" in str(exc)
    else:
        raise AssertionError("unknown method must fail closed")
