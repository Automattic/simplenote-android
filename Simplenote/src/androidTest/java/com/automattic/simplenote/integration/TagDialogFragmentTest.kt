package com.automattic.simplenote.integration

import androidx.fragment.app.testing.launchFragment
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.automattic.simplenote.BaseUITest
import com.automattic.simplenote.R
import com.automattic.simplenote.TagDialogFragment
import org.junit.Assert
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class TagDialogFragmentTest : BaseUITest() {

    private fun launchFragment(tagName: String) {
        val scenario = launchFragment(null, R.style.Base_Theme_Simplestyle) {
            val tag = getTag(tagName)
            TagDialogFragment(
                    tag,
                    notesBucket,
                    tagsBucket
            )
        }

        // Validates the dialog is shown
        scenario.onFragment { fragment ->
            Assert.assertNotNull(fragment.dialog)
            Assert.assertEquals(true, fragment.requireDialog().isShowing)
            fragment.parentFragmentManager.executePendingTransactions()
        }
    }
}
