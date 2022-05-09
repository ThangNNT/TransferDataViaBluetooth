package com.nnt.transferdataviabluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*




class BluetoothService(private val bluetoothAdapter: BluetoothAdapter, private val handler: Handler) {
    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? =null
    private var listener: ConnectionListener? = null
    var currentDevice: BluetoothDevice? = null
        get() {
            if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
                return field
            }
            return null
        }
    private set
    var connectionState: ConnectionState = ConnectionState.NONE
        private set(value) {
        field = value
        listener?.onStateChanged(field)
        Log.d(TAG, connectionState.name)
    }

    @Synchronized fun start(){
        connectionState = ConnectionState.START
        connectThread?.cancel()
        connectThread = null

        connectedThread?.cancel()
        connectedThread = null

        acceptThread?.cancel()
        acceptThread = AcceptThread()
        acceptThread?.start()
    }

    @Synchronized fun connect(bluetoothDevice: BluetoothDevice){
        currentDevice = bluetoothDevice
        connectionState = ConnectionState.CONNECTING
        connectThread?.cancel()
        connectThread = null
        connectedThread?.cancel()
        connectedThread = null
        connectThread = ConnectThread(bluetoothDevice)
        connectThread?.start()
    }

    @Synchronized fun connected(bluetoothSocket: BluetoothSocket){
        currentDevice = bluetoothSocket.remoteDevice
        connectionState = ConnectionState.CONNECTED
        //connectThread?.cancel()
        //connectThread = null
        //acceptThread?.cancel()
        //acceptThread = null
        connectedThread?.cancel()
        connectedThread = null
        connectedThread = ConnectedThread(bluetoothSocket)
        connectedThread?.start()
    }

    @Synchronized fun stop(){
        connectionState = ConnectionState.NONE
        connectThread?.cancel()
        connectThread = null

        connectedThread?.cancel()
        connectedThread = null

        acceptThread?.cancel()
        acceptThread = null
    }

    fun write(out: ByteArray?) {
        out?.let {
            connectedThread?.write(it)
        }
    }

    fun connectFail(){
        start()
        val message = handler.obtainMessage(MessageType.CONNECT_FAIL.ordinal)
        message.sendToTarget()
    }

    fun connectLost(){
        start()
        val message = handler.obtainMessage(MessageType.CONNECT_LOST.ordinal)
        message.sendToTarget()
    }

    fun setConnectionListener(onStateChanged: (state: ConnectionState)-> Unit){
        this.listener = object: ConnectionListener{
            override fun onStateChanged(state: ConnectionState) {
                onStateChanged.invoke(state)
            }
        }
        connectionState = connectionState
    }

    private inner class AcceptThread : Thread() {
        private var mmServerSocket: BluetoothServerSocket? = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
            BLUETOOTH_SERVER_NAME, UUID.fromString(MY_UUID)
        )

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                if(connectionState!= ConnectionState.CONNECTED){
                    val socket: BluetoothSocket? = try {
                        mmServerSocket?.accept()
                    } catch (e: IOException) {
                        Log.e(TAG, "Socket's accept() method failed", e)
                        null
                    }
                    connectThread?.cancel()
                    connectThread = null
                    socket?.also {
                        connected(bluetoothSocket = it )
                        mmServerSocket?.close()
                    }
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

    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()
            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()
                    connected(bluetoothSocket = socket)
                }
                catch (ex: IOException){
                    //connect fail
                    try {
                        val socket2 = device.javaClass.getMethod(
                            "createRfcommSocket", Int::class.javaPrimitiveType
                        ).invoke(device, 1) as BluetoothSocket
                        socket2.connect()
                        connected(bluetoothSocket = socket2)
                    }
                    catch (ex: Exception){
                        connectFail()
                        Log.e(TAG, "Exception - fallback:", ex)
                    }
                    Log.e(TAG, "Exception: ", ex)
                }
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

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    connectLost()
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MessageType.MESSAGE_READ.ordinal, numBytes, -1,
                    mmBuffer)
                readMsg.sendToTarget()
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MessageType.MESSAGE_WRITE.ordinal, -1, -1, bytes)
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmInStream.close()
                mmOutStream.close()
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    interface ConnectionListener{
        fun onStateChanged(state: ConnectionState)
    }

    enum class MessageType{
        CONNECT_FAIL,
        CONNECT_LOST,
        MESSAGE_WRITE,
        MESSAGE_READ
    }

    enum class ConnectionState{
        NONE,
        START,
        CONNECTING,
        CONNECTED
    }

    companion object {
        private const val BLUETOOTH_SERVER_NAME = "BluetoothServer"
        private const val MY_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66"
        private val TAG = BluetoothService::class.java.simpleName
    }
}