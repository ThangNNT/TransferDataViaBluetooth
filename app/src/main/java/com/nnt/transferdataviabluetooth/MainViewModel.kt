package com.nnt.transferdataviabluetooth

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    private val _newDevice = MutableLiveData<BluetoothDevice>()
    val newDevice: LiveData<BluetoothDevice>  = _newDevice

    fun updateNewDevice(bluetoothDevice: BluetoothDevice) {
        _newDevice.value = bluetoothDevice
    }
}