package net.omisoft.bluetoothexample.adapters

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_bluetooth_device.view.*
import net.omisoft.bluetoothexample.R

class BluetoothDevicesAdapter : RecyclerView.Adapter<BluetoothViewHolder>() {

    var onDeviceClickListener: ((BluetoothDevice) -> Unit)? = null

    private val devices = mutableListOf<BluetoothDevice>()

    fun addDevice(newDevice: BluetoothDevice) {
        if (devices.any { it == newDevice }) return

        val added = devices.add(newDevice)
        if (added) {
            notifyItemInserted(devices.indexOf(newDevice))
        }
    }

    fun clearDevices() {
        devices.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return BluetoothViewHolder(view)
    }

    override fun getItemCount() = devices.size

    override fun onBindViewHolder(holder: BluetoothViewHolder, position: Int) {
        holder.bindView(devices[position]) { onDeviceClickListener?.invoke(it) }
    }
}

class BluetoothViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bindView(device: BluetoothDevice, onDeviceClickListener: (BluetoothDevice) -> Unit) {
        with(itemView) {
            val deviceName = if (device.name.isNullOrEmpty()) {
                device.address
            } else {
                "${device.name} (${device.address})"
            }
            tv_device_name.text = deviceName
            ll_device.setOnClickListener { onDeviceClickListener.invoke(device) }
        }
    }

}