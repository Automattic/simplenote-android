package com.automattic.simplenote.utils

import android.content.res.Resources
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.Root
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers.assertThat
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

fun isToast(): Matcher<Root> {
    return object : TypeSafeMatcher<Root>() {
        override fun matchesSafely(root: Root): Boolean {
            val type = root.windowLayoutParams.get().type
            // TYPE_APPLICATION_OVERLAY hangs the test
            if (type == WindowManager.LayoutParams.TYPE_TOAST) {
                val windowToken = root.decorView.windowToken
                val appToken = root.decorView.applicationWindowToken
                if (windowToken === appToken) {
                    // windowToken == appToken means this window isn't contained by any other windows.
                    // if it was a window for an activity, it would have TYPE_BASE_APPLICATION.
                    return true
                }
            }
            return false
        }

        override fun describeTo(description: Description) {

        }
    }
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
