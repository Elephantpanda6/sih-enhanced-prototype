package com.example.sihscrap.hardware

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

data class ScaleReading(
    val weightKg: Double = 0.0,
    val isStable: Boolean = false,
    val unit: String = "kg",
    val isConnected: Boolean = false,
    val isSimulated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object BleWeightParser {
    fun parse(data: ByteArray): Pair<Double, Boolean> {
        if (data.isEmpty()) return Pair(0.0, false)
        val flags = data[0].toInt()
        val isLbs = (flags and 0x01) != 0

        var weight = 0.0
        if (data.size >= 3) {
            val raw = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            // Standard Bluetooth SIG resolution is 0.005 kg
            weight = raw * 0.005
            if (isLbs) {
                weight *= 0.45359237
            }
        }
        val isStable = (flags and 0x02) == 0
        return Pair(Math.round(weight * 100.0) / 100.0, isStable)
    }
}

class BleScaleService(private val context: Context) {
    private val TAG = "BleScaleService"

    companion object {
        // Standard Bluetooth SIG Weight Scale Service & Characteristic
        val WEIGHT_SCALE_SERVICE_UUID: UUID = UUID.fromString("0000181D-0000-1000-8000-00805f9b34fb")
        val WEIGHT_MEASUREMENT_CHAR_UUID: UUID = UUID.fromString("00002A98-0000-1000-8000-00805f9b34fb")
    }

    private val _readingFlow = MutableStateFlow(ScaleReading())
    val readingFlow: StateFlow<ScaleReading> = _readingFlow.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var simulationJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to BLE Weight Scale GATT Server.")
                gatt?.discoverServices()
                _readingFlow.value = _readingFlow.value.copy(isConnected = true, isSimulated = false)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from BLE Weight Scale.")
                _readingFlow.value = _readingFlow.value.copy(isConnected = false)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(WEIGHT_SCALE_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(WEIGHT_MEASUREMENT_CHAR_UUID)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic?.uuid == WEIGHT_MEASUREMENT_CHAR_UUID) {
                val data = characteristic.value ?: return
                parseWeightData(data)
            }
        }
    }

    fun parseWeightData(data: ByteArray) {
        val (weight, isStable) = BleWeightParser.parse(data)
        _readingFlow.value = ScaleReading(
            weightKg = weight,
            isStable = isStable,
            unit = "kg",
            isConnected = true,
            isSimulated = false
        )
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (adapter == null || !adapter.isEnabled || !hasPermission) {
            Log.d(TAG, "Bluetooth not available or permitted. Activating realistic simulated loadcell fallback.")
            simulateLiveLoadcell(14.5)
            return
        }

        try {
            // Attempt to connect to bonded device with weight scale profile
            val pairedDevices = adapter.bondedDevices
            val scaleDevice = pairedDevices.firstOrNull {
                it.name?.contains("scale", ignoreCase = true) == true ||
                it.name?.contains("weight", ignoreCase = true) == true ||
                it.name?.contains("sih", ignoreCase = true) == true
            }

            if (scaleDevice != null) {
                bluetoothGatt = scaleDevice.connectGatt(context, false, gattCallback)
            } else {
                Log.d(TAG, "No physical BLE scale bonded. Using live loadcell simulation.")
                simulateLiveLoadcell(14.5)
            }
        } catch (e: Exception) {
            Log.w(TAG, "BLE exception: ${e.message}. Using simulated loadcell.")
            simulateLiveLoadcell(14.5)
        }
    }

    fun simulateLiveLoadcell(targetWeightKg: Double = 14.5) {
        simulationJob?.cancel()
        simulationJob = serviceScope.launch {
            _readingFlow.value = ScaleReading(weightKg = 0.0, isStable = false, isConnected = true, isSimulated = true)
            delay(300)

            // Step 1: Fluctuation as material is placed on the scale pan
            val steps = listOf(
                targetWeightKg * 0.45,
                targetWeightKg * 0.85,
                targetWeightKg * 1.04,
                targetWeightKg * 0.98,
                targetWeightKg * 1.01,
                targetWeightKg
            )

            for (w in steps) {
                _readingFlow.value = ScaleReading(
                    weightKg = Math.round(w * 100.0) / 100.0,
                    isStable = false,
                    isConnected = true,
                    isSimulated = true
                )
                delay(250)
            }

            // Step 2: Settled and stabilized weight locked
            _readingFlow.value = ScaleReading(
                weightKg = targetWeightKg,
                isStable = true,
                isConnected = true,
                isSimulated = true
            )
        }
    }

    fun stopListening() {
        simulationJob?.cancel()
        try {
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: Exception) {
            // Safe cleanup
        }
        _readingFlow.value = _readingFlow.value.copy(isConnected = false)
    }
}
