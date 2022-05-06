package com.nnt.transferdataviabluetooth

import android.Manifest
import android.bluetooth.*
import android.content.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.nnt.transferdataviabluetooth.databinding.ActivityMainBinding
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.isVisible


class MainActivity : AppCompatActivity(), DeviceListDialog.Listener {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val devices = LinkedHashMap<String, BluetoothDevice>()
    private var bluetoothService: BluetoothService? = null
    private var messageAdapter: MessageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter= bluetoothManager.adapter
        checkBluetoothStateAndDoAction()
        setupListenBluetoothStateChanged()
        getPairedDevices()

        //listen for device found
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(discoveryReceiver, filter)

        requestNecessaryPermission()
        setupListener()
        binding.rvMessages.apply {

        }

        bluetoothAdapter?.let {
            bluetoothService = BluetoothService(it, handler = mHandler)
            bluetoothService?.setConnectionListener { state ->
                when(state){
                    BluetoothService.ConnectionState.NONE ->{
                        showNoConnectedDevice()
                    }
                    BluetoothService.ConnectionState.CONNECTING ->{
                        showNoConnectedDevice()
                    }
                    BluetoothService.ConnectionState.CONNECTED ->{
                        showDeviceConnected()
                    }
                    else ->{
                        showNoConnectedDevice()
                    }
                }
            }
            bluetoothService?.start()
        }
    }

    private fun showNoConnectedDevice(){
        runOnUiThread {
            binding.layoutNoConnectedDevice.isVisible = true
            binding.layoutSend.isVisible = false
        }
    }
    private fun showDeviceConnected(){
        runOnUiThread {
            binding.layoutNoConnectedDevice.isVisible = false
            binding.layoutSend.isVisible= true
        }
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when(msg.what){
                BluetoothService.MessageType.MESSAGE_TOAST.ordinal -> {
                    Toast.makeText(this@MainActivity, "toast", Toast.LENGTH_SHORT).show()

                }
                BluetoothService.MessageType.MESSAGE_READ.ordinal -> {
                    val readBuf = msg.obj as ByteArray
                    Toast.makeText(this@MainActivity, String(readBuf), Toast.LENGTH_SHORT).show()
                }
                BluetoothService.MessageType.MESSAGE_WRITE.ordinal -> {
                    val writeBuf = msg.obj as ByteArray
                    Toast.makeText(this@MainActivity, String(writeBuf), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage(message: String){
        bluetoothService?.write(message.toByteArray())
    }

    private val registerDiscoverableDevice =  registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        //do something
    }
    private fun requestDiscoverableDevice(){
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        registerDiscoverableDevice.launch(discoverableIntent)
    }

    private fun getPairedDevices(){
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            devices[device.address] = device
        }
    }

    private fun requestNecessaryPermission(){
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERM_REQUEST_CODE
        )
    }

    private fun setupListener(){
        binding.btnSend.setOnClickListener {
            val message = binding.edtMessage.text.toString()
            if(message.isNotEmpty()){
                binding.edtMessage.setText("")
                sendMessage(message)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == PERM_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//        }
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
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                }
            }
        }
    }

    private val bluetoothStateChangeReceiver = BluetoothStateReceiver()

    private fun setupListenBluetoothStateChanged(){
        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateChangeReceiver, intentFilter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.scan -> {
                if (bluetoothAdapter?.isDiscovering == true){
                    bluetoothAdapter?.cancelDiscovery()
                }
                if (bluetoothAdapter?.startDiscovery() == true) {
                    DeviceListDialog.newInstance(ArrayList(devices.values))
                        .show(supportFragmentManager, " devices")
                }
                true
            }
            R.id.make_discoverable -> {
                requestDiscoverableDevice()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService?.stop()
        unregisterReceiver(bluetoothStateChangeReceiver)
        unregisterReceiver(discoveryReceiver)
    }

    companion object {
        private const val PERM_REQUEST_CODE = 1222
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onItemClick(bluetoothDevice: BluetoothDevice) {
        val device = bluetoothAdapter?.getRemoteDevice(bluetoothDevice.address)
        device?.let {
            bluetoothService?.connect(device)
        }
    }
}