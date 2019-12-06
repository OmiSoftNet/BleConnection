package net.omisoft.bluetoothexample

import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.omisoft.bluetoothexample.models.BluetoothStatus
import net.omisoft.bluetoothexample.utils.DeviceManager
import net.omisoft.bluetoothexample.models.DeviceType
import net.omisoft.bluetoothexample.utils.showFragment
import net.omisoft.bluetoothexample.utils.showFragmentAsRoot

class MainActivity : AppCompatActivity(), BleActivity {

    private lateinit var deviceManager: DeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceManager = DeviceManager(this)
        supportFragmentManager.showFragmentAsRoot(R.id.container, SearchDevicesFragment.newInstance())
    }

    override fun checkBluetooth(): BluetoothStatus = deviceManager.checkBluetooth()

    override fun startSearchDevices(
        deviceType: DeviceType,
        devicesCallback: (BluetoothDevice) -> Unit
    ) {
        deviceManager.startSearchDevices(deviceType, devicesCallback)
    }

    override fun onDeviceClicked(device: BluetoothDevice, deviceType: DeviceType) {
        deviceManager.stopSearchDevices()
        supportFragmentManager.showFragment(R.id.container, DeviceConnectionFragment.newInstance(deviceType, device))
    }

    override fun connectDevice(device: BluetoothDevice, listener: DeviceManager.DeviceStatusListener) {
        deviceManager.setDeviceStatusListener(listener)
        deviceManager.connectDevice(device)
    }

    override fun startScanDevice() {
        deviceManager.startScanPulse()
    }

    override fun disconnectDevice() {
        deviceManager.closeConnection()
        deviceManager.setDeviceStatusListener(null)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.size >= 1) {
            supportFragmentManager.showFragmentAsRoot(R.id.container, SearchDevicesFragment.newInstance())
        } else {
            super.onBackPressed()
        }
    }
}

