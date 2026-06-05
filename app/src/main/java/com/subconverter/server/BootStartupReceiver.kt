package com.subconverter.server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subconverter.core.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootStartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val appContext = context.applicationContext
                val container = AppContainer.get(appContext)
                val settings = container.settingsStore.current()
                if (settings.autoStartOnBoot) {
                    container.settingsStore.update(settings.copy(enabled = true))
                    LocalHttpServerService.start(appContext)
                }
            }
            pendingResult.finish()
        }
    }
}
