package com.nnt.transferdataviabluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BluetoothStateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action?.equals(BluetoothAdapter.ACTION_STATE_CHANGED)== true){
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when(state){
                BluetoothAdapter.STATE_OFF ->{
                    context?.let {
                        Toast.makeText(it, "Bluetooth turned off", Toast.LENGTH_SHORT).show()
                    }
                }
                BluetoothAdapter.STATE_ON ->{
                    context?.let {
                        Toast.makeText(it, "Bluetooth turned on", Toast.LENGTH_SHORT).show()
                    }
                }
                else ->{

                }
            }
        }
    }
}