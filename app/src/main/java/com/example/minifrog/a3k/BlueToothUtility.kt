package com.example.minifrog.a3k

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Message
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.system.exitProcess


/**
 * Created by mini-frog on 2018/4/3.
 *
 * 有关于蓝牙的操作都在里面
 */

class BlueToothUtility(private val context: Context, private val viewController: BaseHandler) {

    var myBluetoothAdapter: BluetoothAdapter? = null

    var blueToothAddress: String? = null

    private var blueToothPassword: String? = null

    var blueToothDevice: BluetoothDevice? = null

    private var blueToothDeviceRunning = false

    private val myBluetoothReceiver = MyBroadcastReceiver()

    private var inStream: InputStream? = null

    private var outStream: OutputStream? = null

    private var searchBlueToothTimeout = false

    private var timeoutReadWriteLock = ReentrantReadWriteLock()

    private var deviceReadWriteLock = ReentrantReadWriteLock()

    init {
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (myBluetoothAdapter == null) {
            exitProcess(0)
        }
        requestBluetooth()
        val intent = IntentFilter(BluetoothDevice.ACTION_FOUND)
        intent.addAction(BluetoothDevice.ACTION_FOUND)//搜索发现设备
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(myBluetoothReceiver, intent)
    }

    inner class MyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {//每扫描到一个设备，系统都会发送此广播。
                //获取蓝牙设备
                val scanDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (scanDevice == null || scanDevice.name == null || scanDevice.address != blueToothAddress) return
                if (myBluetoothAdapter!!.isDiscovering)
                    myBluetoothAdapter!!.cancelDiscovery()
                messageSearchBlueToothSuccess()
                deviceReadWriteLock.writeLock().lock()
                blueToothDevice = scanDevice
                deviceReadWriteLock.writeLock().unlock()
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                if (blueToothDevice == null) {
                    messageSearchBlueToothFail()
                    timeoutReadWriteLock.writeLock().lock()
                    searchBlueToothTimeout = true
                    timeoutReadWriteLock.writeLock().unlock()
                }
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == action) {
                messageStartSearchBlueTooth()
            }
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                if ((! myBluetoothAdapter!!.isEnabled) && myBluetoothAdapter!!.isDiscovering) {
                    if (blueToothDevice == null) {
                        myBluetoothAdapter!!.cancelDiscovery()
                        messageSearchBlueToothFail()
                        timeoutReadWriteLock.writeLock().lock()
                        searchBlueToothTimeout = true
                        timeoutReadWriteLock.writeLock().unlock()
                    }
                }
            }
        }
    }

    private fun connectBlueTooth(): Boolean {
        messageStartBlueToothConnection()
        try {
            if (blueToothDevice!!.bondState == BluetoothDevice.BOND_NONE) {
                blueToothDevice!!.createBond()
            }
            val blueToothSocket = blueToothDevice!!.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            blueToothSocket.connect()
            outStream = blueToothSocket.outputStream
            inStream = blueToothSocket.inputStream
            messageBluetoothConnectionSuccess()
            return true
        } catch (error: IOException) {
            messageBluetoothConnectionFail(1)
            blueToothDevice = null
            outStream = null
            inStream = null
            error.printStackTrace()
        }
        return false
    }

    fun stopBlueTooth() {
        if (myBluetoothAdapter!!.isDiscovering)
            myBluetoothAdapter!!.cancelDiscovery()
        closeStream()
        context.unregisterReceiver(myBluetoothReceiver)
    }

    private fun blueToothEnable(): Boolean {
        if (!myBluetoothAdapter!!.isEnabled) {
            if (!myBluetoothAdapter!!.enable()) {
                return false
            }
        }
        return true
    }

    private fun requestBluetooth() {
        if (!myBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(enableBtIntent)
        }
    }

    private fun setterCommandGenerator(phoneNumber: String, warm: Boolean): ByteArray {
        return when (warm) {
            true -> phoneNumber.substring(7..10).toByteArray() + blueToothPassword!!.toByteArray() + 0x0F + "\r\n".toByteArray()
            false -> phoneNumber.substring(7..10).toByteArray() + blueToothPassword!!.toByteArray() + 0X08 + "\r\n".toByteArray()
        }
    }

    private fun getterCommandGenerator(): ByteArray {
        return blueToothPassword!!.toByteArray() + 0x00 + "\r\n".toByteArray()
    }

    private fun search() {
        myBluetoothAdapter!!.startDiscovery()
    }

    private fun createConnection(): Boolean {
        if (blueToothDevice == null) {
            searchBlueToothTimeout = false
            search()
        }
        while (true) {
            deviceReadWriteLock.readLock().lock()
            if (blueToothDevice != null) {
                deviceReadWriteLock.readLock().unlock()
                break
            }
            deviceReadWriteLock.readLock().unlock()
            timeoutReadWriteLock.readLock().lock()
            if (searchBlueToothTimeout) {
                timeoutReadWriteLock.readLock().unlock()
                return false
            }
            timeoutReadWriteLock.readLock().unlock()
        }
        if (!connectBlueTooth()) {
            return false
        }
        return true
    }

    fun setterOpenBoxService(phoneNumber: String, warm: Boolean): Boolean {
        if (blueToothDeviceRunning) {
            messageBlueToothServiceRunning()
            return false
        }
        startUsingBlueTooth()

        if (!blueToothEnable()) {
            stopUsingBlueTooth()
            return false
        }

        if (!createConnection()) {
            stopUsingBlueTooth()
            return false
        }
        try {
            messageBlueToothSendData()
            outStream!!.write(setterCommandGenerator(phoneNumber, warm))

            Thread.sleep(2000)
            if (inStream!!.available() == 0) {
                messageBlueToothSendDataTimeout()
                stopUsingBlueTooth()
                return false
            }
            val inBuffer = ByteArray(inStream!!.available())
            inStream!!.read(inBuffer)
            when (inBuffer.last().toString()) {
                "1" -> {
                    messageNowOpeningBox()
                    stopUsingBlueTooth()
                    return true
                }
                "-3" -> {
                    messageNeedClose()
                    stopUsingBlueTooth()
                    return false
                }
                "-2" -> {
                    messageBoxIsCleaning()
                    stopUsingBlueTooth()
                    return false
                }
            }
            return false
        } catch (error: IOException) {
            closeStream()
            messageBlueToothSendDataFail()
        }
        stopUsingBlueTooth()
        return false
    }


    fun getterOpenBoxService(): Boolean {
        if (blueToothDeviceRunning) {
            messageBlueToothServiceRunning()
            return false
        }
        startUsingBlueTooth()

        if (!blueToothEnable()) {
            stopUsingBlueTooth()
            return false
        }

        if (!createConnection()) {
            stopUsingBlueTooth()
            return false
        }
        try {
            messageBlueToothSendData()
            outStream!!.write(getterCommandGenerator())

            Thread.sleep(1000)
            if (inStream!!.available() == 0) {
                messageBlueToothSendDataTimeout()
                stopUsingBlueTooth()
                return false
            }
            val inBuffer = ByteArray(inStream!!.available())
            inStream!!.read(inBuffer)
            when (inBuffer.last().toString()) {
                "1" -> {
                    messageNowOpeningBox()
                    stopUsingBlueTooth()
                    return true
                }
                "-3" -> {
                    messageNeedClose()
                    Thread(Runnable {
                        val networkConnection = NetworkConnection(viewController)
                        val jsonData = JSONObject()
                        jsonData.put("box_name", blueToothAddress)
                        val response = networkConnection.postRequest("report", jsonData.toString())
                    }).start()
                    stopUsingBlueTooth()
                    return true
                }
                "-2" -> {
                    messageBoxIsCleaning()
                    stopUsingBlueTooth()
                    return false
                }
            }
            return false
        } catch (error: IOException) {
            closeStream()
            messageBlueToothSendDataFail()
        }
        stopUsingBlueTooth()
        return false
    }

    fun setBlueTooth(blueToothAddress: String, blueToothPassword: String) {
        this.blueToothAddress = blueToothAddress
        this.blueToothPassword = blueToothPassword
    }

    fun closeBoxSync(): Boolean {
        blueToothDevice?.let {
            inStream?.let {
                val count = it.available()
                if (count > 0) {
                    val buffer = ByteArray(count)
                    it.read(buffer)
                    if (buffer[0].toString() == "2")
                        return true
                }
            }
        }
        return false
    }

    private fun closeStream() {
        inStream?.close()
        outStream?.close()
        inStream = null
        outStream = null
        blueToothDevice = null
    }

    private fun startUsingBlueTooth() {
        blueToothDeviceRunning = true
    }

    private fun stopUsingBlueTooth() {
        blueToothDeviceRunning = false
        closeStream()
    }


    private fun messageStartSearchBlueTooth() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "正在扫描蓝牙"
        viewController.sendMessage(message)
    }

    private fun messageSearchBlueToothSuccess() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "蓝牙扫描成功"
        viewController.sendMessage(message)
    }

    private fun messageSearchBlueToothFail() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "蓝牙扫描失败，请靠近后重试"
        viewController.sendMessage(message)
    }

    private fun messageStartBlueToothConnection() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "正在连接外卖箱蓝牙"
        viewController.sendMessage(message)
    }

    private fun messageBluetoothConnectionSuccess() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "连接外卖箱蓝牙成功"
        viewController.sendMessage(message)
    }

    private fun messageBluetoothConnectionFail(code: Int) {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "连接外卖箱蓝牙失败" + when (code) {
            0 -> "密码错误"
            else -> "未知错误"
        }
        viewController.sendMessage(message)
    }

    private fun messageBlueToothSendData() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "正在发送开启箱门请求"
        viewController.sendMessage(message)
    }

    private fun messageBlueToothSendDataFail() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "信息发送失败"
        viewController.sendMessage(message)
    }

    private fun messageBluetoothCloseBoxTimeout() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "关闭箱门超时"
        viewController.sendMessage(message)
    }

    private fun messageBlueToothSendDataTimeout() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "发送信息等待超时"
        viewController.sendMessage(message)
    }

    private fun messageBlueToothServiceRunning() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "服务正在进行"
        viewController.sendMessage(message)
    }

    private fun messageBoxIsCleaning() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "箱子正在清洁"
        viewController.sendMessage(message)
    }

    private fun messageNowOpeningBox() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "正在打开箱门，取出后请记得关闭箱门"
        viewController.sendMessage(message)
    }

    private fun messageNeedClose() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "需要先关闭箱门"
        viewController.sendMessage(message)
    }
}