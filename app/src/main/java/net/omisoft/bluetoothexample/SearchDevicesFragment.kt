package net.omisoft.bluetoothexample

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.search_device_fragment.*
import net.omisoft.bluetoothexample.adapters.BluetoothDevicesAdapter
import net.omisoft.bluetoothexample.models.BluetoothStatus
import net.omisoft.bluetoothexample.models.DeviceType

class SearchDevicesFragment : Fragment() {

    private lateinit var bleActivity: BleActivity

    private lateinit var itemDivider: DividerItemDecoration

    private lateinit var rxPermissions: RxPermissions

    private val devicesAdapter = BluetoothDevicesAdapter().apply {
        onDeviceClickListener = { bleActivity.onDeviceClicked(it, getSelectedDeviceType()) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BleActivity) {
            bleActivity = context
        }

        itemDivider = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        rxPermissions = RxPermissions(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.search_device_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        initBottomNavigation()
        initRecyclerView()
    }

    override fun onStop() {
        rv_devices.removeItemDecoration(itemDivider)
        super.onStop()
    }

    private fun initBottomNavigation() {
        bn_main.setOnNavigationItemSelectedListener {
            val scanningDevicesType = when (it.itemId) {
                R.id.bottom_navigation_all -> DeviceType.ALL
                R.id.bottom_navigation_mi_band -> DeviceType.MI_BAND
                R.id.bottom_navigation_heart_rate -> DeviceType.HEART_RATE_MONITOR
                else -> DeviceType.ALL
            }

            devicesAdapter.clearDevices()
            scanDevices(scanningDevicesType)
            true
        }
        bn_main.selectedItemId = R.id.bottom_navigation_all
    }

    private fun startScanDevices(deviceType: DeviceType) {
        progress_circular.visibility = View.VISIBLE
        bleActivity.startSearchDevices(deviceType, ::onBleDeviceFound)
    }

    private fun showBluetoothDisabledError() {
        context?.let {
            AlertDialog.Builder(it).apply {
                setTitle(R.string.bluetooth_turn_off)
                setMessage(R.string.bluetooth_turn_off_description)
                setCancelable(false)
                setPositiveButton(R.string.bluetooth_enable) { _, _ ->
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, RC_ENABLE_BLUETOOTH)
                }
                show()
            }
        }
    }

    private fun showBluetoothNotFoundError() {
        context?.let {
            AlertDialog.Builder(it).apply {
                setTitle(R.string.bluetooth_do_not_support)
                setMessage(R.string.bluetooth_do_not_support_description)
                setCancelable(false)
                setNegativeButton(R.string.done) { _, _ -> }
                show()
            }
        }
    }

    private fun onBleDeviceFound(device: BluetoothDevice) {
        progress_circular.visibility = View.GONE
        devicesAdapter.addDevice(device)
    }

    private fun showTurnOnGpsAlert() {
        context?.let {
            AlertDialog.Builder(it).apply {
                setMessage(R.string.gps_on_reason)
                setCancelable(false)
                setPositiveButton(R.string.settings) { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent, RC_ENABLE_BLUETOOTH)
                }
                show()
            }
        }
    }

    @SuppressLint("CheckResult")
    fun scanDevices(deviceType: DeviceType) {
        rxPermissions
                .request(Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe { granted ->
                    if (granted) {
                        val locationManager =
                                context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val gpsEnabled =
                                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)
                                        ?: false
                        if (gpsEnabled) {

                            when (bleActivity.checkBluetooth()) {
                                BluetoothStatus.ENABLED -> startScanDevices(deviceType)
                                BluetoothStatus.DISABLED -> showBluetoothDisabledError()
                                BluetoothStatus.NOT_FOUND -> showBluetoothNotFoundError()
                            }
                        } else {
                            showTurnOnGpsAlert()
                        }
                    } else {
                        Toast.makeText(context, R.string.cannot_get_location_permission, Toast.LENGTH_LONG)
                                .show()
                    }
                }
    }

    private fun initRecyclerView() {
        rv_devices.layoutManager = LinearLayoutManager(context)
        rv_devices.adapter = devicesAdapter
        rv_devices.addItemDecoration(itemDivider)
    }

    private fun getSelectedDeviceType(): DeviceType =
            when (bn_main.selectedItemId) {
                R.id.bottom_navigation_all -> DeviceType.ALL
                R.id.bottom_navigation_mi_band -> DeviceType.MI_BAND
                R.id.bottom_navigation_heart_rate -> DeviceType.HEART_RATE_MONITOR
                else -> DeviceType.ALL
            }

    companion object {
        private const val RC_ENABLE_BLUETOOTH = 101

        fun newInstance(): Fragment {
            return SearchDevicesFragment()
        }
    }
}