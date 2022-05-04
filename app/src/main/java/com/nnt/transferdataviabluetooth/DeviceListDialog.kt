package com.nnt.transferdataviabluetooth

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nnt.transferdataviabluetooth.databinding.DialogDevicesBinding

class DeviceListDialog: BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by viewModels({ requireActivity()})

    private val devices by lazy {
        arguments?.getParcelableArrayList<BluetoothDevice>(DEVICES)
    }

    private var deviceAdapter: DeviceListAdapter? = null

    private lateinit var binding: DialogDevicesBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogDevicesBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup()
    }

    private fun setup(){
        val map = LinkedHashMap<String, BluetoothDevice>()
        devices?.forEach {
            map[it.address] = it
        }
        binding.ivClose.setOnClickListener {
            dismiss()
        }
        deviceAdapter = DeviceListAdapter(devices?: ArrayList()){
            if(requireActivity() is Listener){
                val listener = (requireActivity() as Listener)
                listener.onItemClick(bluetoothDevice = it)
                dismiss()
            }
        }
        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = deviceAdapter
        }

        viewModel.newDevice.observe(requireActivity()){
            if(!map.contains(it.address)){
                map[it.address] = it
                deviceAdapter?.appendDevice(it)
            }
        }
    }

    interface Listener {
        fun onItemClick(bluetoothDevice: BluetoothDevice)
    }

    companion object {
        private const val DEVICES = "DEVICES"
        fun newInstance(devices: ArrayList<BluetoothDevice>) = DeviceListDialog().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(DEVICES, devices)
            }
        }
    }
}