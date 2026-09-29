package com.automattic.simplenote.utils

import android.content.res.Resources
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher


fun hasTextInputLayoutErrorText(expectedErrorText: String): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun matchesSafely(view: View): Boolean {
            if (view !is TextInputLayout) {
                return false
            }

            val error = view.error ?: return false
            val errorStr = error.toString()

            return expectedErrorText == errorStr
        }

        override fun describeTo(description: Description) {

        }
    }
}

class RecyclerViewMatcher(private val recyclerViewId: Int) {
    fun atPositionOnView(position: Int, targetViewId: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            var resources: Resources? = null
            var childView: View? = null

            override fun describeTo(description: Description) {
                var idDescription = recyclerViewId.toString()
                if (this.resources != null) {
                    idDescription = try {
                        this.resources!!.getResourceName(recyclerViewId)
                    } catch (var4: Resources.NotFoundException) {
                        String.format("%s (resource name not found)",
                                Integer.valueOf(recyclerViewId)
                        )
                    }

                }

                description.appendText("with id: $idDescription")
            }

            override fun matchesSafely(view: View): Boolean {

                this.resources = view.resources

                if (childView == null) {
                    val recyclerView = view.rootView.findViewById(recyclerViewId) as RecyclerView
                    if (recyclerView.id == recyclerViewId) {
                        childView = recyclerView.findViewHolderForAdapterPosition(position)!!.itemView
                    } else {
                        return false
                    }
                }

                return if (targetViewId == -1) {
                    view === childView
                } else {
                    val targetView = childView!!.findViewById<View>(targetViewId)
                    view === targetView
                }
            }
        }
    }
}

fun withRecyclerView(recyclerViewId: Int): RecyclerViewMatcher = RecyclerViewMatcher(recyclerViewId)

private const val TOAST_TIMEOUT_MS = 5000L

/**
 * Runs [action] and fails with a [java.util.concurrent.TimeoutException] if it does not show a toast with [text].
 *
 * Since API 30, the system UI draws text toasts outside of the app window hierarchy, so Espresso root
 * matchers cannot find them. The toast still sends an accessibility event, which [android.app.UiAutomation]
 * receives.
 */
fun assertToastShown(text: String, action: () -> Unit) {
    InstrumentationRegistry.getInstrumentation().uiAutomation.executeAndWaitForEvent(
        { action() },
        { event ->
            event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
                event.text.any { it.toString() == text }
        },
        TOAST_TIMEOUT_MS
    )
}

class RecyclerViewItemCountAssertion(private val matcher: Matcher<Int>) : ViewAssertion {
    override fun check(view: View?, noViewFoundException: NoMatchingViewException?) {
        if (noViewFoundException != null) {
            throw noViewFoundException
        }
        val recyclerView = view as RecyclerView
        val adapter = recyclerView.adapter
        assertThat(adapter!!.itemCount, matcher)
    }
}

fun withItemCount(expectedCount: Int): RecyclerViewItemCountAssertion {
    return withItemCount(`is`(expectedCount))
}

fun withItemCount(matcher: Matcher<Int>): RecyclerViewItemCountAssertion {
    return RecyclerViewItemCountAssertion(matcher)
}
