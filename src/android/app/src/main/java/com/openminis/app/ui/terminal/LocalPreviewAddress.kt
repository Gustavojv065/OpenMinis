package com.openminis.app.ui.terminal

/**
 * The Android PRoot shell shares the device network namespace with Android.
 * Opening the loopback URL in the existing in-app preview does not require
 * exposing a development server to the public Internet or to the LAN.
 *
 * Accept *only* a TCP port, never user-provided hosts, paths or schemes.
 */
internal object LocalPreviewAddress {
    private val numericPort = Regex("^[0-9]{1,5}$")

    fun fromPortInput(raw: String): String? {
        val value = raw.trim()
        if (!numericPort.matches(value)) return null
        val port = value.toIntOrNull() ?: return null
        if (port !in 1..65535) return null
        return "http://127.0.0.1:$port/"
    }
}
