package com.f1telemetry.viewer.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.coroutines.coroutineContext

/**
 * Listens for F1 telemetry UDP datagrams on [port] (game default 20777) and
 * hands each payload to [onPacket]. Runs until the coroutine is cancelled.
 */
class UdpTelemetryReceiver(private val port: Int = 20777) {

    suspend fun listen(
        onError: (Throwable) -> Unit,
        onPacket: (buf: ByteArray, length: Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                soTimeout = 1000
                bind(InetSocketAddress(port))
            }
            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)
            while (coroutineContext.isActive) {
                try {
                    socket.receive(packet)
                    onPacket(packet.data, packet.length)
                    packet.length = buffer.size
                } catch (e: java.net.SocketTimeoutException) {
                    // idle tick, loop and re-check cancellation
                } catch (e: Exception) {
                    if (coroutineContext.isActive) onError(e)
                }
            }
        } catch (e: Exception) {
            onError(e)
        } finally {
            socket?.close()
        }
    }
}
