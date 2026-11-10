package com.wanlok.scraper.data

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/** An IPv4 address of the phone and the network interface that has it, such as wlan0. */
data class IpAddress(val interfaceName: String, val address: String)

object IpAddresses {
    /** The phone's IPv4 addresses on the networks that are up, not counting loopback. */
    fun list(): List<IpAddress> {
        // Null when the phone has no network interfaces at all.
        val networks = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        return Collections.list(networks)
            .filter { it.isUp && !it.isLoopback }
            .flatMap { network ->
                Collections.list(network.inetAddresses)
                    .filterIsInstance<Inet4Address>()
                    .map { IpAddress(network.name, it.hostAddress.orEmpty()) }
            }
    }
}