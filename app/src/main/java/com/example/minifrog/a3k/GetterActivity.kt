package com.example.minifrog.a3k

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Message
import android.view.View
import android.widget.Button
import org.json.JSONObject

/**
 * Created by mini-frog on 2018/4/13.
 *
 * Getter food activity
 */

class GetterActivity : Activity() {

    private var getFoodButton: Button? = null
    private var refreshFoodButton: Button? = null

    private var getterViewController = GetterViewController(this, this)

    private var networkConnection = NetworkConnection(getterViewController)

    private var bluetoothUtility: BlueToothUtility? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        System.setProperty("http.keepAlive", "false")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_getter)
        val userInfo = intent.getStringArrayListExtra("user_info")
        networkConnection.phone = userInfo[0]
        networkConnection.password = userInfo[1]
        networkConnection.cookie = intent.getStringArrayListExtra("cookie")

        bluetoothUtility = BlueToothUtility(this, getterViewController)
        initView()
        refreshMessage()

        if (intent.getStringExtra("is_close_door") == "true") {
            messageLastTimeNotCloseDoor()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothUtility!!.stopBlueTooth()
    }

    private fun initView() {
        val getterClickListener = GetterClickListener()
        getFoodButton = findViewById(R.id.get_food)
        refreshFoodButton = findViewById(R.id.refresh_food)
        getFoodButton!!.setOnClickListener(getterClickListener)
        refreshFoodButton!!.setOnClickListener(getterClickListener)
    }


    internal class GetterViewController(private var context: Context, private var getterActivity: GetterActivity) : BaseHandler(context) {

        companion object {
            const val REMIND_MESSAGE = 0
            const val GET_FOOD_DISABLE_MESSAGE = 1
            const val GET_FOOD_ENABLE_MESSAGE = 2
            const val GET_FOOD_BUTTON_LOCK_MESSAGE = 3
            const val GET_FOOD_BUTTON_UNLOCK_MESSAGE = 4
        }

        override fun handleMessage(msg: Message?) {
            msg?.let {
                super.handleMessage(msg)
                when (msg.what) {
                    GET_FOOD_ENABLE_MESSAGE -> {
                        getterActivity.getFoodButton!!.isClickable = true
                        getterActivity.getFoodButton!!.background =
                                context.getDrawable(R.drawable.button_touchable_background)
                        remind("您有一份外卖尚未拿取")
                    }
                    GET_FOOD_DISABLE_MESSAGE -> {
                        getterActivity.getFoodButton!!.isClickable = false
                        getterActivity.getFoodButton!!.background =
                                context.getDrawable(R.drawable.button_untouchable_background)
                        remind("尚未有您的外卖")
                    }
                    GET_FOOD_BUTTON_LOCK_MESSAGE -> {
                        getterActivity.getFoodButton!!.isClickable = false
                        getterActivity.getFoodButton!!.background =
                                context.getDrawable(R.drawable.button_untouchable_background)
                    }
                    GET_FOOD_BUTTON_UNLOCK_MESSAGE -> {
                        getterActivity.getFoodButton!!.isClickable = true
                        getterActivity.getFoodButton!!.background =
                                context.getDrawable(R.drawable.button_touchable_background)
                    }
                }
            }
        }
    }

    internal inner class GetterClickListener : View.OnClickListener {

        override fun onClick(button: View) {
            when (button.id) {
                R.id.refresh_food -> refreshMessage()
                R.id.get_food -> getFoodBoxInfo()
            }
        }
    }

    fun getFoodBoxInfo() {
        Thread(Runnable {
            try {
                messageGetFoodButtonLock(true)
                val receiveData = networkConnection.postRequest("food/get", "")
                receiveData?.let {
                    val receiveJson = JSONObject(it)
                    if (receiveJson.get("status") == "true" && receiveJson.get("code") == "0") {
                        //messageGetFoodBoxSuccess()

                        bluetoothUtility!!.setBlueTooth(receiveJson.get("box_name").toString(), receiveJson.get("box_password").toString())
                        if (!bluetoothUtility!!.getterOpenBoxService()) {
                            messageGetFoodButtonLock(false)
                            return@Runnable
                        }
                        getFoodSuccess()
                        return@Runnable
                    }
                    messageGetFoodBoxFail((receiveJson.get("code") as String).toInt())
                    messageGetFoodButtonLock(false)
                    return@Runnable
                }
                messageGetFoodButtonLock(false)
            } catch (e: Exception) {
                messageGetFoodBoxFail(Int.MAX_VALUE)
                e.printStackTrace()
                messageGetFoodButtonLock(false)
            }
        }).start()
    }


    private fun getFoodSuccess() {
        Thread(Runnable {
            try {
                val receiveData = networkConnection.postRequest("food/getSuccess", "")
                receiveData?.let {
                    val receiveJson = JSONObject(it)
                    if (receiveJson.get("status") == "true" && receiveJson.get("code") == "0") {
                        //messageGetFoodSuccess()
                    } else {
                        messageGetFoodFail((receiveJson.get("code") as String).toInt())
                    }
                    return@Runnable
                }
            } catch (e: Exception) {
                messageGetFoodBoxFail(Int.MAX_VALUE)
                e.printStackTrace()
            }
        }).start()
    }


    fun refreshMessage() {
        Thread(Runnable {
            try {
                val receiveData = networkConnection.postRequest("food/refresh", "")
                receiveData?.let {
                    val receiveJson = JSONObject(it)
                    if (receiveJson.get("status") == "true" && receiveJson.get("code") == "0") {
                        if (receiveJson.get("have") == "true") {
                            messageGetFoodEnable()
                        } else {
                            messageGetFoodDisable()
                        }
                    } else {
                        messageGetFoodBoxFail((receiveJson.get("code") as String).toInt())
                    }
                    return@Runnable
                }
            } catch (e: Exception) {
                messageGetFoodBoxFail(Int.MAX_VALUE)
                e.printStackTrace()
            }
        }).start()
    }


    fun logout() {
        networkConnection.cleanCookie()
        openFileOutput("register.txt", Context.MODE_PRIVATE).close()
    }

    override fun onBackPressed() {
        logout()
        super.onBackPressed()
    }

    private fun messageGetFoodSuccess() {
        val message = Message()
        message.what = GetterViewController.REMIND_MESSAGE
        message.obj = "服务器同步成功"
        getterViewController.sendMessage(message)
    }

    private fun messageGetFoodFail(code: Int) {
        val message = Message()
        message.what = GetterViewController.REMIND_MESSAGE
        message.obj = "服务器同步失败" + when (code) {
            1 -> "未以普通用户身份登陆"
            2 -> "尚未有您的外卖"
            else -> "未知的错误"
        }
        getterViewController.sendMessage(message)
    }

    private fun messageGetFoodButtonLock(lock: Boolean) {
        val message = Message()
        message.what = when (lock) {
            true -> GetterViewController.GET_FOOD_BUTTON_LOCK_MESSAGE
            false -> GetterViewController.GET_FOOD_BUTTON_UNLOCK_MESSAGE
        }
        getterViewController.sendMessage(message)
    }

    private fun messageGetFoodEnable() {
        val message = Message()
        message.what = GetterViewController.GET_FOOD_ENABLE_MESSAGE
        getterViewController.sendMessage(message)
    }

    private fun messageGetFoodDisable() {
        val message = Message()
        message.what = GetterViewController.GET_FOOD_DISABLE_MESSAGE
        getterViewController.sendMessage(message)
    }

    private fun messageGetFoodBoxSuccess() {
        val message = Message()
        message.what = GetterViewController.REMIND_MESSAGE
        message.obj = "箱门信息获取成功"
        getterViewController.sendMessage(message)
    }

    private fun messageGetFoodBoxFail(code: Int) {
        val message = Message()
        message.what = GetterViewController.REMIND_MESSAGE
        message.obj = "箱门信息获取失败" + when (code) {
            1 -> "未以普通用户身份登陆"
            2 -> "尚未有您的外卖"
            else -> "未知的错误"
        }
        getterViewController.sendMessage(message)
    }

    private fun messageLastTimeNotCloseDoor() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "上一次使用外卖箱没有关闭箱门"
        getterViewController.sendMessage(message)
    }
}