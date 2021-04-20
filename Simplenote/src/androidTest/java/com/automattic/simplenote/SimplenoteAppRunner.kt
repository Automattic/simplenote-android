package com.automattic.simplenote

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class SimplenoteAppRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, SimplenoteTest::class.java.name, context)
    }
}
