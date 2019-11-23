package net.omisoft.bluetoothexample

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_device_connection.*
import net.omisoft.bluetoothexample.models.DeviceType
import net.omisoft.bluetoothexample.utils.AllGattServices
import net.omisoft.bluetoothexample.utils.DeviceManager


class DeviceConnectionFragment : Fragment() {

    private lateinit var bleActivity: BleActivity

    private lateinit var deviceType: DeviceType

    private var device: BluetoothDevice? = null

    private var initialActionBarColor = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_device_connection, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let { attachedActivity ->
            if (attachedActivity is BleActivity) {
                bleActivity = attachedActivity
            } else {
                Log.e(TAG, "Dialog have to be attached to the BleActivity.")
                attachedActivity.onBackPressed()
            }
        }

        parseArguments(savedInstanceState)
        initViews()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.apply {
            initialActionBarColor = window?.statusBarColor ?: 0
            window?.statusBarColor = ContextCompat.getColor(this, R.color.colorAccent)
        }
    }

    override fun onStart() {
        super.onStart()
        bleActivity.connectDevice(device!!, object : DeviceManager.DeviceStatusListener {
            override fun onConnect(gatt: BluetoothGatt) {
                Log.d(TAG, "On device connected")
                activity?.runOnUiThread {
                    updateConnectionState(DeviceConnectionState.CONNECTED)
                }
                val servicesBuilder = StringBuilder()

                val deviceCanMeasurePulse =
                        gatt.services.none {
                            // Mi Band is Hidden device and all characteristics could be read from the certified apps only
                            it.uuid == DeviceManager.MI_BAND_UUID
                        } && gatt.services.any { service ->
                            service.uuid == DeviceManager.HR_MONITOR_SERVICE_UUID
                                    && service.uuid != DeviceManager.MI_BAND_UUID
                                    && service.characteristics.any { it.uuid == DeviceManager.HR_CHARACTERISTICS_HEART_RATE_UUID }
                        }

                activity?.runOnUiThread {
                    if (deviceCanMeasurePulse) {
                        btn_start_scan.visibility = View.VISIBLE
                    } else {

                        btn_start_scan.visibility = View.INVISIBLE
                    }
                }

                gatt.services
                        .map { it.uuid }
                        .forEach {
                            val serviceName = AllGattServices.lookup(it)
                            servicesBuilder.append(serviceName).append("\n")
                        }

                activity?.runOnUiThread {
                    showDeviceAvailableServices(servicesBuilder.toString())
                }
            }

            override fun onDataReceived(characteristic: BluetoothGattCharacteristic) {
                Log.d(TAG, "On data received")
                activity?.runOnUiThread {
                    showReceivedData(characteristic.value)
                }
            }

            override fun onDisconnect() {
                activity?.runOnUiThread {
                    updateConnectionState(DeviceConnectionState.DISCONNECTED)
                }
            }
        })
    }

    override fun onDestroyView() {
        activity?.window?.statusBarColor = initialActionBarColor
        super.onDestroyView()
    }

    private fun showDeviceAvailableServices(services: String) {
        tv_gatt_services?.text = services
    }

    private fun showReceivedData(data: ByteArray) {
        if (data.isNotEmpty()) {
            tv_gatt_state?.text = data[1].toString()
            tv_gatt_state?.visibility = View.VISIBLE
        }
    }

    private fun updateConnectionState(connectionState: DeviceConnectionState) {
        when (connectionState) {
            DeviceConnectionState.CONNECTING -> {
                tv_connection_state?.setText(R.string.connect_device_screen_connecting)
            }
            DeviceConnectionState.CONNECTED -> {
                tv_connection_state?.setText(R.string.connect_device_screen_connected)
            }
            DeviceConnectionState.DISCONNECTED -> {
                tv_connection_state?.setText(R.string.connect_device_screen_disconnected)
            }
        }
    }

    override fun onStop() {
        bleActivity.disconnectDevice()
        super.onStop()
    }

    private fun parseArguments(savedInstanceState: Bundle?) {
        val params = savedInstanceState ?: arguments
        params?.let {
            deviceType = it.getSerializable(ARG_DEVICE_TYPE) as DeviceType
            device = it.getParcelable(ARG_BLUETOOTH_DEVICE) as? BluetoothDevice
        }
    }

    private fun initViews() {
        val deviceAddress = device?.address ?: ""
        val deviceName = device?.name ?: ""
        val title = if (deviceName.isBlank()) {
            deviceAddress
        } else {
            "$deviceName\n$deviceAddress"
        }

        btn_start_scan.setOnClickListener {
            btn_start_scan.visibility = View.GONE
            bleActivity.startScanDevice()
        }

        tv_title?.text = title
        btn_close?.setOnClickListener {
            updateConnectionState(DeviceConnectionState.DISCONNECTED)
            activity?.onBackPressed()
        }
        updateConnectionState(DeviceConnectionState.CONNECTING)
    }

    companion object {
        private val TAG = DeviceConnectionFragment::class.java.simpleName

        private const val ARG_DEVICE_TYPE = "ARG_DEVICE_TYPE"
        private const val ARG_BLUETOOTH_DEVICE = "ARG_BLUETOOTH_DEVICE"

        fun newInstance(deviceType: DeviceType, device: BluetoothDevice): Fragment {
            return DeviceConnectionFragment().apply {
                arguments = Bundle().also {
                    it.putParcelable(ARG_BLUETOOTH_DEVICE, device)
                    it.putSerializable(ARG_DEVICE_TYPE, deviceType)
                }
            }
        }
    }
}

enum class DeviceConnectionState {
    CONNECTING, CONNECTED, DISCONNECTED
}