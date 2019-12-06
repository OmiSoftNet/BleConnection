package net.omisoft.bluetoothexample.utils

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import net.omisoft.bluetoothexample.models.BluetoothStatus
import net.omisoft.bluetoothexample.models.DeviceType
import java.lang.ref.WeakReference
import java.util.*

class DeviceManager(context: Context) {

    private val contextReference = WeakReference(context)

    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    // To provide bluetooth communication
    private var bluetoothGatt: BluetoothGatt? = null
    private var deviceStatusListener: DeviceStatusListener? = null

    private var scanCallback: ScanCallback? = null

    companion object {
        private val TAG = DeviceManager::class.java.simpleName

        val MI_BAND_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb")

        val HR_MONITOR_SERVICE_UUID = toUUID(0x180d)
        val HR_CHARACTERISTICS_HEART_RATE_UUID = toUUID(0x2a37)

        private fun toUUID(value: Int): UUID {
            val msb = 0x0000000000001000L
            val lsb = -0x7fffff7fa064cb05L
            val result = (value and -0x1).toLong()
            return UUID(msb or (result shl 32), lsb)
        }
    }

    /**
     * Add listener to receive scanned data
     *
     * @see DeviceStatusListener
     */
    fun setDeviceStatusListener(listener: DeviceStatusListener?) {
        this.deviceStatusListener = listener
    }

    /**
     * Check is Bluetooth LE is available and is it turned on
     *
     * @return current state of Bluetooth scanner
     * @see BluetoothStatus
     */
    fun checkBluetooth(): BluetoothStatus {
        val hasSupportLe = contextReference.get()
                ?.packageManager
                ?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
                ?: false

        return if (bluetoothAdapter == null || !hasSupportLe) {
            BluetoothStatus.NOT_FOUND
        } else if (!bluetoothAdapter?.isEnabled!!) {
            BluetoothStatus.DISABLED
        } else {
            BluetoothStatus.ENABLED
        }
    }

    /**
     * Start searching Bluetooth LE devices according to the selected device type
     * and return one by one found devices via devicesCallback
     *
     * @param deviceType type of devices for searching
     * @param devicesCallback a callback for found devices
     *
     * @see DeviceType
     */
    fun startSearchDevices(deviceType: DeviceType, devicesCallback: (BluetoothDevice) -> Unit) {
        stopSearchDevices()

        val deviceFilter = ScanFilter.Builder()
                .apply {
                    if (deviceType != DeviceType.ALL) {
                        setServiceUuid(ParcelUuid(getScanServiceUuid(deviceType)))
                    }
                }
                .build()

        val bluetoothSettings = ScanSettings.Builder().build()

        scanCallback = object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val scanRecord = result.scanRecord

                if (scanRecord != null) {
                    Log.d(TAG, String.format("Device Result: %s", result.toString()))
                    devicesCallback.invoke(result.device)
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                for (sr in results) {
                    Log.d(TAG, String.format("ScanResult - Results: %s", sr.toString()))
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, String.format("Scan Failed. Error Code: %s", errorCode))
            }
        }

        bluetoothAdapter?.bluetoothLeScanner
                ?.startScan(
                        mutableListOf(deviceFilter),
                        bluetoothSettings,
                        scanCallback
                )
    }

    /**
     * Stop Bluetooth LE scanning process
     */
    fun stopSearchDevices() {
        bluetoothAdapter?.isDiscovering?.let {
            bluetoothAdapter?.cancelDiscovery()
        }
        scanCallback?.let { bluetoothAdapter?.bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
    }

    /**
     * Arrange connection to the selected device, and read characteristics of the identified device type
     *
     * @param device instance of BluetoothDevice that was received during scanning process
     *
     * @see DeviceType
     */
    fun connectDevice(device: BluetoothDevice) {
        if (isDeviceConnected()) {
            Log.e(TAG, "Only 1 device can be connected at a time")
            return
        }

        bluetoothGatt = device.connectGatt(
                contextReference.get(),
                false,
                object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        Log.d(TAG, "onConnectionStateChange Status: $status")
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                Log.d(TAG, "Device Connected")
                                gatt.discoverServices()
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                Log.e(TAG, "Device Disconnected")

                                deviceStatusListener?.onDisconnect()
                                closeConnection()
                            }
                        }
                        when (status) {
                            BluetoothGatt.GATT_FAILURE -> {
                                Log.e(TAG, "Device connection failure")

                                deviceStatusListener?.onDisconnect()
                                closeConnection()
                            }
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        for (service in gatt.services) {
                            Log.d(TAG, String.format("Service UUID: %s (%s)",
                                    AllGattServices.lookup(service.uuid),
                                    service.uuid))

                            for (characteristic in service.characteristics) {
                                Log.d(TAG, String.format("Service CHARACT UUID: %s (%s)",
                                        AllGattCharacteristics.lookup(characteristic.uuid),
                                        characteristic.uuid))

                                for (descriptor in characteristic.descriptors) {
                                    Log.d(TAG, String.format("Service DESCRIPTOR UUID: %s (%s)",
                                            AllGattDescriptors.lookup(descriptor.uuid),
                                            descriptor.uuid))
                                }
                            }
                        }

                        deviceStatusListener?.onConnect(gatt)
                    }

                    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                        Log.d(TAG, String.format("onCharacteristicRead %s, status=%d", characteristic.uuid, status))
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            handleCharacteristics(characteristic)
                        }
                    }

                    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                        Log.d(TAG, String.format("onCharacteristicChanged %s", characteristic.uuid))
                        handleCharacteristics(characteristic)
                    }

                    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
                        Log.d(TAG, "Scanner onDescriptorWrite")
                        descriptor?.characteristic?.let { bluetoothGatt?.readCharacteristic(it) }
                    }
                })
    }

    /**
     * Close connection with earlier connected device
     */
    fun closeConnection() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    /**
     * Start scanning changes of the appropriate characteristic from the connected device.
     * All changed will be sent to DeviceStatusListener
     *
     * @see DeviceStatusListener
     * @see setDeviceStatusListener
     */
    fun startScanPulse() {
        Log.d(TAG, "Scanner: Start Scan")

        var pulseCharacteristic: BluetoothGattCharacteristic? = null
        bluetoothGatt?.services?.forEach servicesLoop@ { service ->
            service.characteristics.forEach { characteristic ->
                if (characteristic.uuid == HR_CHARACTERISTICS_HEART_RATE_UUID) {
                    pulseCharacteristic = characteristic
                    return@servicesLoop
                }
            }
        }
        pulseCharacteristic?.let {
            setCharacteristicNotification(it, true)
        }
    }

    private fun isDeviceConnected() = bluetoothGatt != null

    private fun getScanServiceUuid(device: DeviceType): UUID? =
            when (device) {
                DeviceType.MI_BAND -> MI_BAND_UUID
                DeviceType.HEART_RATE_MONITOR -> HR_MONITOR_SERVICE_UUID
                else -> null
            }

    private fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enable: Boolean) {
        // Enable notifications for this characteristic locally
        bluetoothGatt?.setCharacteristicNotification(characteristic, enable)

        // Write on the config descriptors to be notified when the value changes
        characteristic?.descriptors?.forEach { descriptor ->
            descriptor?.let {
                it.value = if (enable) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                bluetoothGatt?.writeDescriptor(it)
            }
        }
    }

    private fun handleCharacteristics(characteristic: BluetoothGattCharacteristic) {
        when (characteristic.uuid) {
            HR_CHARACTERISTICS_HEART_RATE_UUID -> {
                deviceStatusListener?.onDataReceived(characteristic)
            }
        }
    }

    interface DeviceStatusListener {
        fun onConnect(gatt: BluetoothGatt)

        fun onDataReceived(characteristic: BluetoothGattCharacteristic)

        fun onDisconnect()
    }
}