package com.subconverter

import android.app.Application
import com.subconverter.core.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SubConverterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.outputRepository.ensureBuiltinTemplates()
        }
    }
}
