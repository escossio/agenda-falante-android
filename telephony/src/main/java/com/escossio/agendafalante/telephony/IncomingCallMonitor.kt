package com.escossio.agendafalante.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import java.util.concurrent.Executor

class IncomingCallMonitor(
    context: Context,
    private val callbacks: Callbacks,
) {
    private val appContext = context.applicationContext
    private val telephonyManager = appContext.getSystemService(TelephonyManager::class.java)
    private var started = false
    private var legacyListener: PhoneStateListener? = null
    private var modernCallback: TelephonyCallback? = null

    fun start() {
        if (started) return
        if (!hasReadPhoneStatePermission()) {
            callbacks.onPermissionRequired()
            return
        }
        if (telephonyManager == null) {
            callbacks.onUnavailable("Telephony service not available.")
            return
        }
        started = true
        callbacks.onMonitoring()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startModern()
        } else {
            startLegacy()
        }
    }

    fun stop() {
        if (!started) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = modernCallback
            if (callback != null) {
                telephonyManager?.unregisterTelephonyCallback(callback)
            }
            modernCallback = null
        } else {
            legacyListener?.let { telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE) }
            legacyListener = null
        }
        started = false
        callbacks.onDisabled()
    }

    private fun startModern() {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallState(state)
            }
        }
        modernCallback = callback
        telephonyManager?.registerTelephonyCallback(directExecutor(), callback)
    }

    @Suppress("DEPRECATION")
    private fun startLegacy() {
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleCallState(state)
            }
        }
        legacyListener = listener
        telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun handleCallState(state: Int) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            callbacks.onRinging()
        }
    }

    private fun hasReadPhoneStatePermission(): Boolean {
        return appContext.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    private fun directExecutor(): Executor = Executor { runnable -> runnable.run() }

    interface Callbacks {
        fun onMonitoring()
        fun onPermissionRequired()
        fun onUnavailable(reason: String)
        fun onDisabled()
        fun onRinging()
    }
}
