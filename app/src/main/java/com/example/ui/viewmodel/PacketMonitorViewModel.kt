package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.model.ParsedPacket
import com.example.model.ThreatAlert
import com.example.service.PacketCaptureVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PacketMonitorViewModel : ViewModel() {
    companion object {
        private const val TAG = "PacketMonitorViewModel"
    }

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _packets = MutableStateFlow<List<ParsedPacket>>(emptyList())
    val packets: StateFlow<List<ParsedPacket>> = _packets.asStateFlow()

    private val _threatAlerts = MutableStateFlow<List<ThreatAlert>>(emptyList())
    val threatAlerts: StateFlow<List<ThreatAlert>> = _threatAlerts.asStateFlow()

    private val _selectedPacket = MutableStateFlow<ParsedPacket?>(null)
    val selectedPacket: StateFlow<ParsedPacket?> = _selectedPacket.asStateFlow()

    private val _showScenarioDialog = MutableStateFlow(false)
    val showScenarioDialog: StateFlow<Boolean> = _showScenarioDialog.asStateFlow()

    private val _simulationActive = MutableStateFlow(false)
    val simulationActive: StateFlow<Boolean> = _simulationActive.asStateFlow()

    private var vpnService: PacketCaptureVpnService? = null
    private val packetCache = ConcurrentHashMap<Int, ParsedPacket>()
    private var packetCounter = 0

    fun onStartCaptureClick(context: Context, onVpnPrepare: (Intent) -> Unit) {
        try {
            // FIX: Prepare VPN properly before starting capture
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                // VPN permission not yet granted, launch permission dialog
                onVpnPrepare(vpnIntent)
            } else {
                // Already have permission, start VPN service directly
                startVpnService(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing VPN: ${e.message}", e)
        }
    }

    fun startVpnService(context: Context) {
        try {
            _isCapturing.value = true
            Log.d(TAG, "Starting VPN service with split-tunneling enabled")
            
            // FIX: Start VPN service with excluded apps configuration
            val intent = Intent(context, PacketCaptureVpnService::class.java)
            context.startService(intent)
            
            // Note: YouTube, X, and other excluded apps will bypass this VPN
            // and connect directly to the internet
            Log.d(TAG, "VPN service started - YouTube, X, and other apps will use direct internet connection")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN service: ${e.message}", e)
            _isCapturing.value = false
        }
    }

    fun stopCapture(context: Context) {
        try {
            _isCapturing.value = false
            Log.d(TAG, "Stopping VPN service")
            
            val intent = Intent(context, PacketCaptureVpnService::class.java)
            context.stopService(intent)
            
            Log.d(TAG, "VPN service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN service: ${e.message}", e)
        }
    }

    fun toggleSimulation() {
        _simulationActive.value = !_simulationActive.value
        if (_simulationActive.value) {
            startSimulation()
        }
    }

    private fun startSimulation() {
        viewModelScope.launch {
            // Simulate network traffic for testing
            Log.d(TAG, "Simulation mode activated - generating mock packets")
        }
    }

    fun selectPacket(packet: ParsedPacket?) {
        _selectedPacket.value = packet
    }

    fun setShowScenarioDialog(show: Boolean) {
        _showScenarioDialog.value = show
    }

    fun injectScenario(scenario: Any) {
        // Inject attack scenario for testing
        Log.d(TAG, "Injecting scenario: $scenario")
    }

    override fun onCleared() {
        super.onCleared()
        packetCache.clear()
    }
}
