package com.wanlok.scraper.server

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address

object NetworkAddress {
    /** The phone's IPv4 address on the current network (usually Wi-Fi), or null when offline. */
    fun localIpv4(context: Context): String? {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return null
        val properties = manager.getLinkProperties(network) ?: return null
        return properties.linkAddresses
            .map { it.address }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }
}