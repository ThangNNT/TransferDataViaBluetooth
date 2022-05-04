package com.nnt.transferdataviabluetooth

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nnt.transferdataviabluetooth.databinding.ItemDeviceBinding
import java.util.ArrayList

class DeviceListAdapter(private var devices: ArrayList<BluetoothDevice>, val onItemClick: (bluetoothDevice: BluetoothDevice)-> Unit): RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemDeviceBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = parent.context.getSystemService(LayoutInflater::class.java)
        val binding = ItemDeviceBinding.inflate(inflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvDeviceName.text = devices[position].name?: "Unknown"
        holder.binding.root.setOnClickListener {
            onItemClick.invoke(devices[position])
        }
    }

    override fun getItemCount() = devices.size

    fun appendDevice(device: BluetoothDevice){
        devices.add(device)
        notifyItemInserted(devices.size-1)
    }
}