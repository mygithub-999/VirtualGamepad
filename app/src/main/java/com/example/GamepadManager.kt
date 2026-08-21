package com.example

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.P)
@SuppressLint("MissingPermission")
class GamepadManager(private val context: Context) {
    private val TAG = "GamepadManager"
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private val callbackExecutor = Executors.newSingleThreadExecutor()
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    // Single pre-allocated ByteArray for memory management (Flyweight pattern) to avoid GC pauses.
    // 8 bytes:
    // Byte 0: Buttons 1 (A, B, X, Y, L1, R1, L2, R2)
    // Byte 1: Buttons 2 (Select, Start, L3, R3, D-pad...)
    // Byte 2: Left Stick X
    // Byte 3: Left Stick Y
    // Byte 4: Right Stick X
    // Byte 5: Right Stick Y
    // Byte 6: L2 Analog
    // Byte 7: R2 Analog
    private val reportData = ByteArray(8)
    private var isRegistered = false

    private val descriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(), // Usage (Gamepad)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), //   Report ID (1)
        // Buttons
        0x05.toByte(), 0x09.toByte(), //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(), //   Usage Minimum (1)
        0x29.toByte(), 0x10.toByte(), //   Usage Maximum (16)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x10.toByte(), //   Report Count (16)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        // Sticks (Left X, Y, Right X, Y)
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //   Usage (X)
        0x09.toByte(), 0x31.toByte(), //   Usage (Y)
        0x09.toByte(), 0x32.toByte(), //   Usage (Z - Right X)
        0x09.toByte(), 0x35.toByte(), //   Usage (Rz - Right Y)
        0x15.toByte(), 0x81.toByte(), //   Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), //   Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x95.toByte(), 0x04.toByte(), //   Report Count (4)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        // L2/R2 Analog
        0x05.toByte(), 0x01.toByte(), //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x33.toByte(), //   Usage (Rx - L2)
        0x09.toByte(), 0x34.toByte(), //   Usage (Ry - R2)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0xFF.toByte(), //   Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x95.toByte(), 0x02.toByte(), //   Report Count (2)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        // Haptics/Output
        0x85.toByte(), 0x02.toByte(), //   Report ID (2)
        0x09.toByte(), 0x01.toByte(), //   Usage (Vendor specific for Rumble)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0xFF.toByte(), //   Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x95.toByte(), 0x02.toByte(), //   Report Count (2)
        0x91.toByte(), 0x02.toByte(), //   Output (Data, Variable, Absolute)
        0xC0.toByte()                 // End Collection
    )

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            isRegistered = registered
            Log.d(TAG, "App status changed: registered=$registered")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            super.onConnectionStateChanged(device, state)
            hostDevice = if (state == BluetoothProfile.STATE_CONNECTED) device else null
            _connectionState.value = state
            Log.d(TAG, "Connection state changed: $state")
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            super.onSetReport(device, type, id, data)
            // Reverse Data Flow: Haptics & Rumble
            if (id.toInt() == 2 && data.size >= 2) {
                val strongMotor = data[0].toInt() and 0xFF
                val weakMotor = data[1].toInt() and 0xFF
                if (strongMotor > 0 || weakMotor > 0) {
                    val effect = VibrationEffect.createOneShot(100, (strongMotor.coerceAtLeast(weakMotor) * 255 / 255))
                    vibrator?.vibrate(effect)
                }
            }
            // Acknowledge report
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
        }
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "Bluetooth turned off, updating state")
                    _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                    // Fallback to IP Control would be triggered here in UI
                }
            }
        }
    }

    fun start() {
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    fun stop() {
        hidDevice?.let {
            if (isRegistered) {
                it.unregisterApp()
            }
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        context.unregisterReceiver(receiver)
    }

    private fun registerApp() {
        if (hidDevice == null || isRegistered) return
        val subclass = BluetoothHidDevice.SUBCLASS2_GAMEPAD
        val sdpSettings = try {
            BluetoothHidDeviceAppSdpSettings::class.java.getConstructor(
                String::class.java,
                String::class.java,
                String::class.java,
                Byte::class.javaPrimitiveType,
                ByteArray::class.java
            ).newInstance(
                "Virtual Gamepad",
                "AI Studio",
                "Gamepad Provider",
                subclass,
                descriptor
            ) as BluetoothHidDeviceAppSdpSettings
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        hidDevice?.registerApp(sdpSettings, null, null, callbackExecutor, hidCallback)
    }

    fun connectTo(device: BluetoothDevice) {
        if (!isRegistered) return
        hidDevice?.connect(device)
    }

    // Input state updating
    fun updateState(buttons1: Byte, buttons2: Byte, leftX: Byte, leftY: Byte, rightX: Byte, rightY: Byte, l2: Byte, r2: Byte) {
        reportData[0] = buttons1
        reportData[1] = buttons2
        reportData[2] = leftX
        reportData[3] = leftY
        reportData[4] = rightX
        reportData[5] = rightY
        reportData[6] = l2
        reportData[7] = r2
        sendReport()
    }

    fun resetState() {
        updateState(0, 0, 0, 0, 0, 0, 0, 0)
    }

    private fun sendReport() {
        val device = hostDevice ?: return
        coroutineScope.launch {
            hidDevice?.sendReport(device, 1, reportData)
        }
    }
}
// test
