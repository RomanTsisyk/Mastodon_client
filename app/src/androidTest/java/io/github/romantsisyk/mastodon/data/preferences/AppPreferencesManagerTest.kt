package io.github.romantsisyk.mastodon.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AppPreferencesManagerTest {

    private lateinit var preferencesManager: AppPreferencesManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mastodon_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        preferencesManager = AppPreferencesManager(context)
    }

    @Test
    fun given_a_new_app_installation_when_checking_first_launch_status_then_first_launch_should_be_true() {
        // Given
        // When
        val isFirstLaunch = preferencesManager.isFirstLaunch()

        // Then
        assertTrue(isFirstLaunch, "First launch status should be true for a new installation")
    }

    @Test
    fun given_first_launch_is_in_progress_when_marking_first_launch_complete_then_first_launch_status_becomes_false() {
        // Given
        assertTrue(preferencesManager.isFirstLaunch())

        // When
        preferencesManager.markFirstLaunchComplete()

        // Then
        assertFalse(preferencesManager.isFirstLaunch())
    }

    @Test
    fun given_first_launch_is_marked_complete_when_creating_new_preferences_manager_instance_then_first_launch_status_remains_false() {
        // Given
        preferencesManager.markFirstLaunchComplete()

        // When
        val newPreferencesManager = AppPreferencesManager(context)

        // Then
        assertFalse(newPreferencesManager.isFirstLaunch())
    }
}