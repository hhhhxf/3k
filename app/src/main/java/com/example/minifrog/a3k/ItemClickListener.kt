package com.example.minifrog.a3k

/**
 * Created by mini-frog on 2018/4/11.
 *
 * 接口
 */
interface ItemClickListener {
    /**
     * 展开子Item
     * @param bean
     */
    fun onExpandChildren(bean: DataBean)

    /**
     * 隐藏子Item
     * @param bean
     */
    fun onHideChildren(bean: DataBean)
}