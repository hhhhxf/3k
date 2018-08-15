package com.example.minifrog.a3k

import android.os.Message
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by mini-frog on 2018/4/9.
 *
 * 网络连接的工具类
 */

class NetworkConnection(private val baseHandler: BaseHandler){

    companion object {
        const val ADDRESS = "http://139.199.173.64"
    }

    var phone: String? = null

    var password: String? = null

    var type: String? = null

    var cookie: List<String>? = null

    fun postRequest(route: String, data: String): String? {
        var postConnection: HttpURLConnection? = null
        try {
            // 创建URL连接
            val url: URL = when (route != "") {
                true -> URL("$ADDRESS/$route")
                false -> URL(ADDRESS)
            }
            // 初始化连接
            postConnection = url.openConnection() as HttpURLConnection
            postConnection.requestMethod = "POST"
            postConnection.connectTimeout = 8000
            postConnection.readTimeout = 8000
            postConnection.useCaches = false
            postConnection.doOutput = true
            postConnection.doInput = true
            postConnection.setRequestProperty("Content-Type", "application/json")
            cookie?.let {
                val sessionId = it[0].split(";")[0]
                postConnection.setRequestProperty("cookie", sessionId)
            }

            // 设置数据
            postConnection.setRequestProperty("Content-Length", data.length.toString())
            val outStream = postConnection.outputStream
            outStream.write(data.toByteArray())
            outStream.flush()

            // 发起连接后读取数据
            if (postConnection.responseCode == 200) {
                cookie = postConnection.headerFields["Set-Cookie"]
                val inStream = postConnection.inputStream
                val message = ByteArrayOutputStream()
                var len: Int
                val buffer = ByteArray(1024)
                while (true) {
                    len = inStream.read(buffer)
                    if (len == -1)
                        break
                    message.write(buffer, 0, len)
                }
                // 释放资源
                inStream.close()
                message.close()
                // 返回字符串
                return String(message.toByteArray())
            }
        } catch (error: Exception) {
            error.printStackTrace()
            messageServerFail()
        } finally {
            postConnection?.disconnect()
        }
        return null
    }

    fun cleanCookie() {
        cookie = null
    }

    fun getUserInfo() : ArrayList<String> {
        val carryString = java.util.ArrayList<String>()
        carryString.add(phone.toString())
        carryString.add(password.toString())
        return carryString
    }

    fun getCookieArrayList() : ArrayList<String> {
        return ArrayList(cookie)
    }

    private fun messageServerFail() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "服务器连接错误"
        baseHandler.sendMessage(message)
    }
}