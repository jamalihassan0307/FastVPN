package com.example.vpn_basic_project

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.net.VpnService
import android.content.Intent
import android.util.Log
import android.os.Handler
import android.os.Looper

class MainActivity: FlutterActivity() {
    private val CHANNEL = "vpn_engine"
    private val TAG = "VPNEngine"
    private val REQUEST_VPN_PERMISSION = 1

    companion object {
        private var methodChannel: MethodChannel? = null

        fun updateVpnStatus(status: String) {
            Handler(Looper.getMainLooper()).post {
                methodChannel?.invokeMethod("updateStatus", status)
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "startVpn" -> {
                    try {
                        Log.d(TAG, "Preparing to start VPN")
                        val config = call.argument<String>("config")
                        if (config == null) {
                            throw Exception("VPN configuration is null")
                        }

                        // Check VPN permission
                        val vpnIntent = VpnService.prepare(context)
                        if (vpnIntent != null) {
                            Log.d(TAG, "Requesting VPN permission")
                            startActivityForResult(vpnIntent, REQUEST_VPN_PERMISSION)
                            result.error("PERMISSION_DENIED", "VPN permission required", null)
                            return@setMethodCallHandler
                        }

                        // Start VPN service
                        Log.d(TAG, "Starting VPN service")
                        val serviceIntent = Intent(this, OpenVpnService::class.java)
                        serviceIntent.putExtra("config", config)
                        startForegroundService(serviceIntent)
                        result.success(null)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting VPN: ${e.message}")
                        result.error("VPN_START_ERROR", e.message, null)
                    }
                }
                "stopVpn" -> {
                    try {
                        Log.d(TAG, "Stopping VPN")
                        val serviceIntent = Intent(this, OpenVpnService::class.java)
                        stopService(serviceIntent)
                        result.success(null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping VPN: ${e.message}")
                        result.error("VPN_STOP_ERROR", e.message, null)
                    }
                }
                "prepare" -> {
                    try {
                        Log.d(TAG, "Preparing VPN")
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            startActivityForResult(intent, REQUEST_VPN_PERMISSION)
                        }
                        result.success(null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error preparing VPN: ${e.message}")
                        result.error("VPN_PREPARE_ERROR", e.message, null)
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_VPN_PERMISSION) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "VPN permission granted")
                // Notify Flutter about permission granted
                MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, CHANNEL)
                    .invokeMethod("onVpnPermissionGranted", null)
            } else {
                Log.d(TAG, "VPN permission denied")
                // Notify Flutter about permission denied
                MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, CHANNEL)
                    .invokeMethod("onVpnPermissionDenied", null)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
} 