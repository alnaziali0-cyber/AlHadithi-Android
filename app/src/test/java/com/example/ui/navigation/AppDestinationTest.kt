package com.example.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The application navigation is a closed set of destinations; the routes must stay unique and
 * stable because they are also used as the saved-state keys of the shell.
 */
class AppDestinationTest {

    @Test
    fun `every destination has a unique route`() {
        val routes = AppDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `no route is blank`() {
        AppDestination.entries.forEach { destination ->
            assertTrue("blank route for ${destination.name}", destination.route.isNotBlank())
        }
    }

    @Test
    fun `the main sections of the product are reachable`() {
        val routes = AppDestination.entries.map { it.route }.toSet()
        listOf(
            "home",
            "quran",
            "library",
            "duas_ziyarat",
            "more_hub",
            "prayer_times",
            "tasbih",
            "qibla",
            "audio_library",
            "ethics",
            "search",
            "favorites",
            "settings"
        ).forEach { route ->
            assertTrue("missing destination: $route", routes.contains(route))
        }
    }
}
