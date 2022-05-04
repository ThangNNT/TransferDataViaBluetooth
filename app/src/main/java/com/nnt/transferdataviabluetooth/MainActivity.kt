package com.nnt.transferdataviabluetooth

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.nnt.transferdataviabluetooth.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


class MainActivity : AppCompatActivity(), DeviceListDialog.Listener {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val devices = LinkedHashMap<String, BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter= bluetoothManager.adapter
        checkBluetoothStateAndDoAction()
        setupListenBluetoothStateChanged()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            devices[device.address] = device
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(discoveryReceiver, filter)

//        val requestCode = 1;
//        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
//            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
//        }
//        startActivityForResult(discoverableIntent, requestCode)

        AcceptThread().start()
        binding.btnScan.setOnClickListener {
            //start discovery
            if(bluetoothAdapter?.startDiscovery() == true){
                DeviceListDialog.newInstance(ArrayList(devices.values)).show(supportFragmentManager," devices")
            }
        }
    }

    private fun checkBluetoothStateAndDoAction(){
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Your device doesn't support bluetooth!", Toast.LENGTH_SHORT).show()
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetoothResult.launch(enableBtIntent)
        }
    }

    private val requestEnableBluetoothResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == RESULT_OK){
            Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "Bluetooth is disable", Toast.LENGTH_SHORT).show()
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    device?.let {
                        devices[it.address] = it
                        viewModel.updateNewDevice(it)
                    }
                }
            }
        }
    }

    private val bluetoothStateChangeReceiver = BluetoothStateReceiver()

    private fun setupListenBluetoothStateChanged(){
        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateChangeReceiver, intentFilter)
    }

    private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket){
    }

    private inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                applicationContext.getString(
                    R.string.app_name
                ), UUID.fromString(MY_UUID)
            )
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                manageMyConnectedSocket(socket)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateChangeReceiver)
        unregisterReceiver(discoveryReceiver)
    }

    companion object {
        private const val MY_UUID = "1b840124-cb54-11ec-9d64-0242ac120002"
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onItemClick(bluetoothDevice: BluetoothDevice) {
        Log.d("AAAAAAAAAAAAAAAAAAAAA", bluetoothDevice.name)
        ConnectThread(bluetoothDevice).start()
    }
}