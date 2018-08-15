package com.example.minifrog.a3k

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset
import java.util.ArrayList
import kotlin.system.exitProcess


class LoginActivity : AppCompatActivity() {

    private var networkConnection: NetworkConnection? = null

    private var loginViewHandler = LoginViewHandler(this, this)

    private var permissionControl = PermissionControl()

    private var backClickTime = 0L

    private var setterLoginButton: Button? = null
    private var getterLoginButton: Button? = null

    private var phoneEditText: EditText? = null
    private var passwordEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        System.setProperty("http.keepAlive", "false")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initView()

        checkPermission()

        networkConnection = NetworkConnection(loginViewHandler)
        autoLogin()
    }

    override fun onRestart() {
        super.onRestart()
        messageLogout()
        passwordEditText!!.text.clear()
        phoneEditText!!.text.clear()
    }


    private fun initView() {
        val buttonClickListener = LoginClickListener()
        val textWatcher = LoginTextWatcher()
        setterLoginButton = findViewById(R.id.setter_login)
        getterLoginButton = findViewById(R.id.getter_login)
        phoneEditText = findViewById(R.id.phone_number)
        passwordEditText = findViewById(R.id.password)
        setterLoginButton!!.setOnClickListener(buttonClickListener)
        getterLoginButton!!.setOnClickListener(buttonClickListener)
        phoneEditText!!.addTextChangedListener(textWatcher)
        passwordEditText!!.addTextChangedListener(textWatcher)
    }


    internal class LoginViewHandler(private val context: Context,
                                    private val activity: LoginActivity) : BaseHandler(context) {

        companion object {
            const val REMIND_MESSAGE = 0
            const val ENABLE_LOGIN_MESSAGE = 1
            const val DISABLE_LOGIN_MESSAGE = 2
        }

        override fun handleMessage(msg: Message?) {
            msg?.let {
                super.handleMessage(it)
                when (it.what) {
                    ENABLE_LOGIN_MESSAGE -> {
                        activity.setterLoginButton!!.isClickable = true
                        activity.getterLoginButton!!.isClickable = true
                        activity.setterLoginButton!!.background =
                                context.getDrawable(R.drawable.button_touchable_background)
                        activity.getterLoginButton!!.background =
                                context.getDrawable(R.drawable.button_touchable_background)
                    }
                    DISABLE_LOGIN_MESSAGE -> {
                        activity.setterLoginButton!!.isClickable = false
                        activity.getterLoginButton!!.isClickable = false
                        activity.setterLoginButton!!.background =
                                context.getDrawable(R.drawable.button_untouchable_background)
                        activity.getterLoginButton!!.background =
                                context.getDrawable(R.drawable.button_untouchable_background)
                    }
                }
            }
        }
    }


    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backClickTime > 2000) {
            backClickTime = System.currentTimeMillis()
            val message = Message()
            message.what = LoginViewHandler.REMIND_MESSAGE
            message.obj = "再次点击退出程序"
            loginViewHandler.sendMessage(message)
        } else {
            exitProcess(0)
        }
    }

    private fun loginPasswordNotMatch() {
        val message = Message()
        message.what = LoginViewHandler.REMIND_MESSAGE
        message.obj = "登陆失败，密码或用户名错误"
        loginViewHandler.sendMessage(message)
    }

    private fun enableLogin() {
        val message = Message()
        message.what = LoginViewHandler.ENABLE_LOGIN_MESSAGE
        loginViewHandler.sendMessage(message)
    }

    private fun disableLogin() {
        val message = Message()
        message.what = LoginViewHandler.DISABLE_LOGIN_MESSAGE
        loginViewHandler.sendMessage(message)
    }

    private fun messageLogout() {
        val message = Message()
        message.what = LoginViewHandler.REMIND_MESSAGE
        message.obj = "登出成功"
        loginViewHandler.sendMessage(message)
    }


    private fun changeToSetterHome(isCloseDoor: String) {
        val setterActivity = Intent(this, SetterActivity::class.java)
        setterActivity.putStringArrayListExtra("user_info", networkConnection!!.getUserInfo())
        setterActivity.putStringArrayListExtra("cookie", networkConnection!!.getCookieArrayList())
        setterActivity.putExtra("is_close_door", isCloseDoor)
        startActivity(setterActivity)
    }


    private fun changeToGetterHome(isCloseDoor: String) {
        val getterActivity = Intent(this, GetterActivity::class.java)
        getterActivity.putStringArrayListExtra("user_info", networkConnection!!.getUserInfo())
        getterActivity.putStringArrayListExtra("cookie", networkConnection!!.getCookieArrayList())
        getterActivity.putExtra("is_close_door", isCloseDoor)
        startActivity(getterActivity)
    }

    private fun changeToRegister() {

    }

    private fun checkPermission() {
        val permissionString = ArrayList<String>()
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionString.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            permissionControl.permissionLocation = true
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionString.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissionControl.permissionSDCardWrite = true
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionString.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            permissionControl.permissionSDCardRead = true
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionString.add(Manifest.permission.CAMERA)
        } else {
            permissionControl.permissionCamera = true
        }
        if (permissionString.isNotEmpty()) {
            requestPermissions(permissionString.toTypedArray(), 0)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        for (i in permissions.indices) {
            when (permissions[i]) {
                Manifest.permission.ACCESS_COARSE_LOCATION ->
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        permissionControl.permissionLocation = true
                    } else {
                        permissionControl.permissionLocation = false
                        exitProcess(0)
                    }
                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    permissionControl.permissionSDCardRead = when (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        true -> true
                        false -> false
                    }
                }
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    permissionControl.permissionSDCardWrite = when (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        true -> true
                        false -> false
                    }
                }
                Manifest.permission.CAMERA -> {
                    permissionControl.permissionCamera = when (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        true -> true
                        false -> false
                    }
                }
            }
        }
    }

    private fun login(phone: String, password: String, type: String) {
        val jsonData = JSONObject()
        jsonData.put("phone", phone)
        jsonData.put("password", password)
        jsonData.put("type", type)

        Thread(Runnable {
            try {
                val receiveData = networkConnection!!.postRequest("login", jsonData.toString())
                if (receiveData != null) {
                    val receiveJson = JSONObject(receiveData)
                    if (receiveJson.get("status") == "true" && receiveJson.get("code") == "0") {

                        // 设置类的信息
                        networkConnection!!.password = password
                        networkConnection!!.phone = phone
                        networkConnection!!.type = type

                        val loginFile = openFileOutput("register.txt", Context.MODE_PRIVATE)
                        loginFile.write("$phone $password $type".toByteArray())
                        loginFile.close()

                        // 界面转换
                        if (type == "getter") {
                            changeToGetterHome(receiveJson.get("last_time_not_close_door").toString())
                        } else {
                            changeToSetterHome(receiveJson.get("last_time_not_close_door").toString())
                        }
                    } else {
                        loginPasswordNotMatch()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }


    private fun autoLogin() {

        var isFileExist = true

        try {
            openFileInput("register.txt").close()
        } catch (error: IOException) {
            isFileExist = false
        }

        // 在文件不存在的情况下当作第一次打开软件
        if (!isFileExist) {
            return
        }

        // 文件存在时打开文件尝试读取内容
        val loginFile = openFileInput("register.txt")
        val data = ByteArray(1024)
        val dataLength = loginFile.read(data)
        loginFile.close()

        // 查看文件内容是否有效，有效则构造json发起请求
        if (dataLength <= 0) {
            return
        }
        val userInfo = String(data, 0, dataLength, Charset.forName("UTF-8")).split(" ")
        val jsonData = JSONObject()
        jsonData.put("phone", userInfo[0])
        jsonData.put("password", userInfo[1])
        jsonData.put("type", userInfo[2])

        //开启线程来发起网络请求
        Thread(Runnable {
            try {
                val receiveData = networkConnection!!.postRequest("login", jsonData.toString())
                receiveData?.let {
                    val receiveJson = JSONObject(receiveData)

                    if (receiveJson.get("status") == "true" && receiveJson.get("code") == "0") {
                        if (userInfo[2] == "getter")
                            changeToGetterHome(receiveJson.get("last_time_not_close_door").toString())
                        else {
                            changeToSetterHome(receiveJson.get("last_time_not_close_door").toString())
                        }
                    } else {
                        loginPasswordNotMatch()
                    }
                    return@Runnable
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }


    internal inner class LoginClickListener : View.OnClickListener {

        override fun onClick(button: View) {
            when (button.id) {
                R.id.getter_login ->
                    login(phoneEditText!!.text.toString(), passwordEditText!!.text.toString(), "getter")
                R.id.setter_login ->
                    login(phoneEditText!!.text.toString(), passwordEditText!!.text.toString(), "setter")
                R.id.to_register -> {
                    changeToRegister()
                }
            }
        }
    }

    internal inner class LoginTextWatcher : TextWatcher {

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (passwordEditText!!.text.toString() != "" &&
                    phoneEditText!!.text.toString() != "") {
                enableLogin()
            } else {
                disableLogin()
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int,
                                       after: Int) {
        }

        override fun afterTextChanged(s: Editable) {}
    }


}
