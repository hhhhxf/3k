package com.example.minifrog.a3k

import android.content.Context
import android.os.Handler
import android.os.Message
import android.widget.Toast

/**
 * Created by mini-frog on 2018/4/13.
 *
 * My parent class of Handler
 */

abstract class BaseHandler(private val context: Context): Handler() {

    companion object {
        const val REMIND_MESSAGE = 0
    }

    protected fun remind(content: String) {
        lastToast?.cancel()
        val currentToast = Toast.makeText(context, content, Toast.LENGTH_SHORT)
        currentToast.show()
        lastToast = currentToast
    }

    private var lastToast: Toast? = null

    override fun handleMessage(msg: Message?) {
        msg?.let {
            if (it.what == REMIND_MESSAGE) {
                remind(it.obj.toString())
            }
        }
    }

}