package com.escossio.agendafalante.telephony

import android.content.Context
import com.escossio.agendafalante.bridge.IncomingCallEvent
import java.util.UUID

class TelephonyBridge(
    context: Context,
    private val experiencePackageIdProvider: () -> String,
    private val callbacks: Callbacks,
) {
    private val monitor = IncomingCallMonitor(
        context = context,
        callbacks = object : IncomingCallMonitor.Callbacks {
            override fun onMonitoring() {
                callbacks.onStatusChanged(TelephonyStatus.Monitoring)
            }

            override fun onPermissionRequired() {
                callbacks.onStatusChanged(TelephonyStatus.PermissionRequired)
            }

            override fun onUnavailable(reason: String) {
                callbacks.onStatusChanged(TelephonyStatus.Disabled(reason))
            }

            override fun onDisabled() {
                callbacks.onStatusChanged(TelephonyStatus.Disabled())
            }

            override fun onRinging() {
                val event = IncomingCallEvent(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    contactName = "Unknown",
                    experiencePackageId = experiencePackageIdProvider(),
                )
                callbacks.onIncomingCallEvent(event)
            }
        },
    )

    fun start() = monitor.start()

    fun stop() = monitor.stop()

    interface Callbacks {
        fun onStatusChanged(status: TelephonyStatus)
        fun onIncomingCallEvent(event: IncomingCallEvent)
    }
}

sealed class TelephonyStatus {
    data object Monitoring : TelephonyStatus()
    data object PermissionRequired : TelephonyStatus()
    data class Disabled(val reason: String? = null) : TelephonyStatus()
}
