package com.automattic.simplenote

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.FullScreenDialogFragment.FullScreenDialogContent
import com.automattic.simplenote.FullScreenDialogFragment.FullScreenDialogController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The tests call the [androidx.activity.OnBackPressedDispatcher] directly to simulate predictive back gestures.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class FullScreenDialogFragmentTest {
    private lateinit var scenario: ActivityScenario<HiltTestActivity>
    private lateinit var dialog: FullScreenDialogFragment

    @Before
    fun setup() {
        val intent = Intent.makeMainActivity(
            ComponentName(ApplicationProvider.getApplicationContext(), HiltTestActivity::class.java)
        ).putExtra(
            "androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY",
            R.style.Base_Theme_Simplestyle
        )
        scenario = ActivityScenario.launch(intent)

        onActivity { activity ->
            dialog = FullScreenDialogFragment.Builder(activity)
                .setContent(TestContentFragment::class.java, null)
                .build()
            dialog.show(activity.supportFragmentManager, FullScreenDialogFragment.TAG)
        }
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    fun dismissRemovesDialogAndBackStackEntry() {
        onActivity { activity ->
            assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
            dialog.dismiss()
        }

        onActivity { activity ->
            assertFalse(dialog.isAdded)
            assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
        }
    }

    @Test
    fun backGestureAfterDismissDoesNotShowDialogAgain() {
        onActivity { dialog.dismiss() }

        onActivity { activity -> activity.onBackPressedDispatcher.dispatchOnBackStarted(backEvent()) }

        onActivity { activity ->
            assertFalse(dialog.isAdded)
            assertNull(dialog.view)
            activity.onBackPressedDispatcher.dispatchOnBackCancelled()
        }

        onActivity { activity ->
            assertFalse(dialog.isAdded)
            assertNull(dialog.view)
            assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
        }
    }

    @Test
    fun recreatedViewKeepsSingleContentFragment() {
        val content = dialog.content

        onActivity { activity -> activity.supportFragmentManager.beginTransaction().detach(dialog).commitNow() }
        onActivity { activity -> activity.supportFragmentManager.beginTransaction().attach(dialog).commitNow() }

        onActivity {
            assertNotNull(dialog.view)
            assertEquals(listOf(content), dialog.childFragmentManager.fragments)
        }
    }

    @Test
    fun cancelledBackGestureKeepsDialog() {
        val content = dialog.content

        onActivity { activity -> activity.onBackPressedDispatcher.dispatchOnBackStarted(backEvent()) }
        onActivity { activity -> activity.onBackPressedDispatcher.dispatchOnBackCancelled() }

        onActivity { activity ->
            assertTrue(dialog.isResumed)
            assertSame(content, dialog.childFragmentManager.findFragmentById(R.id.full_screen_dialog_fragment_content))
            assertEquals(1, activity.supportFragmentManager.backStackEntryCount)
        }
    }

    @Test
    fun completedBackGestureRemovesDialog() {
        onActivity { activity -> activity.onBackPressedDispatcher.dispatchOnBackStarted(backEvent()) }
        onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }

        onActivity { activity ->
            assertFalse(dialog.isAdded)
            assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
        }
    }

    private fun onActivity(action: (HiltTestActivity) -> Unit) {
        scenario.onActivity { activity ->
            action(activity)
            activity.supportFragmentManager.executePendingTransactions()
        }
    }

    private fun backEvent() = BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT)

    class TestContentFragment : Fragment(), FullScreenDialogContent {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            View(requireContext())

        override fun onConfirmClicked(controller: FullScreenDialogController) = false

        override fun onDismissClicked(controller: FullScreenDialogController) = false

        override fun onViewCreated(controller: FullScreenDialogController) = Unit
    }
}
