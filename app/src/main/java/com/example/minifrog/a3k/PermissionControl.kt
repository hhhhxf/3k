package com.example.minifrog.a3k

import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Created by mini-frog on 2018/4/11.
 */

class PermissionControl {

    var permissionLocation = false

    var permissionSDCardRead = false
    var permissionSDCardWrite = false
    var permissionCamera = false

    var permissionReadWriteLock = ReentrantReadWriteLock()

    fun checkPermissionForCamera() : Boolean {
        if (permissionCamera && permissionSDCardRead && permissionSDCardWrite) {
            return true
        }
        return false
    }
}