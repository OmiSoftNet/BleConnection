package net.omisoft.bluetoothexample

import android.bluetooth.BluetoothDevice
import net.omisoft.bluetoothexample.models.BluetoothStatus
import net.omisoft.bluetoothexample.models.DeviceType
import net.omisoft.bluetoothexample.utils.DeviceManager

interface BleActivity {

    fun checkBluetooth(): BluetoothStatus

    fun startSearchDevices(deviceType: DeviceType, devicesCallback: (BluetoothDevice) -> Unit)

    fun onDeviceClicked(device: BluetoothDevice, deviceType: DeviceType)

    fun connectDevice(device: BluetoothDevice, listener: DeviceManager.DeviceStatusListener)

    fun startScanDevice()

    fun disconnectDevice()

}