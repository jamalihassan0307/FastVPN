package com.example.vpn_basic_project

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class OpenVpnService : VpnService() {
    private val TAG = "OpenVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VPN Service Created")
    }

    fun startVpn(config: String) {
        try {
            Log.d(TAG, "Starting VPN connection")
            
            // Create VPN interface
            val builder = Builder()
                .setSession("OpenVPN")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)

            vpnInterface = builder.establish()
            
            if (vpnInterface != null) {
                Log.d(TAG, "VPN interface established")
                // Notify connection success
                MainActivity.updateVpnStatus("CONNECTED")
            } else {
                Log.e(TAG, "Failed to establish VPN interface")
                MainActivity.updateVpnStatus("FAILED")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN: ${e.message}")
            MainActivity.updateVpnStatus("FAILED")
        }
    }

    fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d(TAG, "VPN stopped")
            MainActivity.updateVpnStatus("DISCONNECTED")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN: ${e.message}")
        }
    }
} 