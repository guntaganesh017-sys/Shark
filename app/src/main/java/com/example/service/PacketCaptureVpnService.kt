package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class PacketCaptureVpnService : VpnService() {
    companion object {
        private const val TAG = "PacketCaptureVpnService"
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "10.0.0.0"
        private const val VPN_ROUTE_PREFIX = 8
        private const val PACKET_BUFFER_SIZE = 32767

        // Apps to exclude from VPN capture (split-tunneling)
        private val EXCLUDED_APPS = setOf(
            "com.twitter.android",           // X (Twitter)
            "com.google.android.youtube",    // YouTube
            "com.instagram.android",         // Instagram
            "com.facebook.katana",           // Facebook
            "com.whatsapp",                  // WhatsApp
            "com.google.android.gms",        // Google Play Services
            "com.android.chrome",            // Chrome
            "com.opera.browser",             // Opera
            "com.UCMobile.intl"              // UC Browser
        )
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        if (isRunning) {
            return START_STICKY
        }
        
        setupVpn()
        return START_STICKY
    }

    private fun setupVpn() {
        try {
            // Build VPN configuration
            val builder = Builder()
            builder.setSession("PacketSharkVPN")
            builder.addAddress(VPN_ADDRESS, VPN_ROUTE_PREFIX)
            builder.addRoute(VPN_ROUTE, VPN_ROUTE_PREFIX)
            builder.addDnsServer("8.8.8.8")
            builder.addDnsServer("8.8.4.4")

            // CRITICAL FIX: Add excluded apps to bypass VPN
            // This prevents the VPN from blocking YouTube, X, and other apps
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                for (excludedApp in EXCLUDED_APPS) {
                    try {
                        builder.addDisallowedApplication(excludedApp)
                        Log.d(TAG, "Excluded app from VPN: $excludedApp")
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w(TAG, "App not found for exclusion: $excludedApp")
                    }
                }
            }

            // Only capture non-excluded apps
            builder.setBlocking(false)

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning = true
                Log.d(TAG, "VPN interface established successfully")
                startPacketCapture()
            } else {
                Log.e(TAG, "Failed to establish VPN interface")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up VPN: ${e.message}", e)
            stopSelf()
        }
    }

    private fun startPacketCapture() {
        scope.launch {
            val vpnFd = vpnInterface ?: return@launch
            try {
                val input = FileInputStream(vpnFd.fileDescriptor)
                val output = FileOutputStream(vpnFd.fileDescriptor)
                val packet = ByteArray(PACKET_BUFFER_SIZE)

                while (isRunning) {
                    try {
                        val len = input.read(packet)
                        if (len > 0) {
                            // Process only packets from non-excluded apps
                            val ipPacket = processIpPacket(packet, len)
                            if (ipPacket != null) {
                                // Send packet back through VPN for allowed apps
                                output.write(ipPacket.data, 0, ipPacket.size)
                                output.flush()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading packet: ${e.message}")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Packet capture error: ${e.message}", e)
            }
        }
    }

    private fun processIpPacket(data: ByteArray, length: Int): ByteArray? {
        return try {
            // Verify minimum IP header size
            if (length < 20) return null

            val buffer = ByteBuffer.wrap(data, 0, length)
            val versionAndHeaderLength = buffer.get().toInt() and 0xFF
            val headerLength = (versionAndHeaderLength and 0x0F) * 4

            if (headerLength < 20 || headerLength > length) return null

            // Return packet for processing by threat detector
            data.copyOf(length)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing IP packet: ${e.message}")
            null
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        isRunning = false
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface: ${e.message}")
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun stopCapture() {
        Log.d(TAG, "stopCapture called")
        isRunning = false
        stopSelf()
    }
}
