package com.example.minifrog.a3k

import android.support.v7.widget.RecyclerView

/**
 * Created by mini-frog on 2018/4/11.
 */

class DataBean(val dataType: Int, var contents: String) {

    companion object {
        const val DATA_TYPE_CHILD = 0
        const val DATA_TYPE_PARENT = 1
    }

    var isExpand = false

    var dataBeanId: Int = 0

    var childDataBean: DataBean? = null

    var parentDataBean: DataBean? = null

    var childViewHolder: RecyclerView.ViewHolder? = null

    var phoneNumberEditable = false

    var phoneNumberValid = false

}