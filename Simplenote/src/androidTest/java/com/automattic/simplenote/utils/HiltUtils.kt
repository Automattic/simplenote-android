package com.automattic.simplenote.utils

import android.content.ComponentName
import android.content.Intent
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.automattic.simplenote.HiltTestActivity
import com.automattic.simplenote.R

/**
 * Source: https://developer.android.com/training/dependency-injection/hilt-testing#launchfragment
 *
 * launchDialogFragmentInHiltContainer from the androidx.fragment:fragment-testing library
 * is NOT possible to use right now as it uses a hardcoded Activity under the hood
 * (i.e. [EmptyFragmentActivity]) which is not annotated with @AndroidEntryPoint.
 *
 * As a workaround, use this function that is equivalent. It requires you to add
 * [HiltTestActivity] in the debug folder and include it in the debug AndroidManifest.xml file
 * as can be found in this project. This function focus on launching DialogFragment
 */

inline fun <reified T : DialogFragment> launchDialogFragmentInHiltContainer(
    @StyleRes themeResId: Int = R.style.Base_Theme_Simplestyle,
    crossinline instantiate: () -> T
): ActivityScenario<HiltTestActivity> {
    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    ).putExtra(
        "androidx.fragment.app.testing.FragmentScenario.EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY",
        themeResId
    )

    val activityScenario: ActivityScenario<HiltTestActivity> = ActivityScenario.launch(startActivityIntent)
    activityScenario.onActivity { activity ->
        val fragment: DialogFragment = instantiate()
        fragment.show(activity.supportFragmentManager.beginTransaction(), "dialog_tag")
    }

    return activityScenario
}