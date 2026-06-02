package com.subconverter

import android.app.Application
import com.subconverter.core.AppContainer

class SubConverterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)
    }
}
