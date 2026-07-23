package com.f1telemetry.viewer.net

import android.content.Context
import android.net.wifi.WifiManager
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
 *
 * A [WifiManager.MulticastLock] is held while listening so the OS does not drop
 * broadcast/multicast datagrams for Wi-Fi power saving — without it, the game's
 * "UDP Broadcast Mode" packets never reach the app.
 */
class UdpTelemetryReceiver(private val port: Int = 20777, private val context: Context? = null) {

    suspend fun listen(
        onError: (Throwable) -> Unit,
        onPacket: (buf: ByteArray, length: Int, senderIp: String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        var lock: WifiManager.MulticastLock? = null
        try {
            val wifi = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            lock = wifi?.createMulticastLock("f1-telemetry")?.apply {
                setReferenceCounted(false)
                runCatching { acquire() }
            }
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                runCatching { broadcast = true }
                soTimeout = 1000
                runCatching { receiveBufferSize = 1 shl 20 }
                bind(InetSocketAddress(port))
            }
            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)
            while (coroutineContext.isActive) {
                try {
                    socket.receive(packet)
                    onPacket(packet.data, packet.length, packet.address?.hostAddress ?: "?")
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
            runCatching { lock?.release() }
        }
    }
}
