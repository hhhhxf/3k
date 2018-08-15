package com.example.minifrog.a3k

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.*
import android.util.Size
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import android.media.ThumbnailUtils
import android.graphics.Bitmap




class CameraActivity : Activity(), View.OnClickListener, TextureView.SurfaceTextureListener {

    private var mTextureView: TextureView? = null
    private var displayView: TextView? = null
    private var surfaceView: RectSurfaceView? = null

    private var childHandler: Handler? = null
    private var mainHandler: Handler? = null
    private var myHandler = MyHandler(this)

    private var mCameraID = "0"//摄像头Id 0 为后  1 为前
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var mPreviewSize: Size? = null

    private var numberRecognize: NumberRecognize? = null

    private var screenShootNeedStop = false

    private var currentRecognizeString = ""
    private var savedString = ArrayList<String>()

    private class MyHandler(val activity: CameraActivity) : Handler() {

        override fun handleMessage(msg: Message) {
            if (msg.what == 1) {
                activity.displayView!!.text = msg.obj.toString()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        initLooper()
        initVIew()
        initRecognize()
    }

    private fun initRecognize() {
        numberRecognize = NumberRecognize(this)
    }


    private fun initVIew() {
        //mSurfaceView
        surfaceView = findViewById(R.id.surface_rect)
        mTextureView = findViewById(R.id.surface_view_camera2_activity)
        mTextureView!!.surfaceTextureListener = this
        displayView = findViewById(R.id.text_preview)
        findViewById<Button>(R.id.confirm).setOnClickListener(this)
    }

    private fun initLooper() {
        val handlerThread = HandlerThread("CAMERA2")
        handlerThread.start()
        mainHandler = Handler(mainLooper)
        childHandler = Handler(handlerThread.looper)
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        try {
            //获得所有摄像头的管理者CameraManager
            val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            //获得某个摄像头的特征，支持的参数
            val characteristics = cameraManager.getCameraCharacteristics(mCameraID)
            //支持的STREAM CONFIGURATION
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            //摄像头支持的预览Size数组
            mPreviewSize = map!!.getOutputSizes(SurfaceTexture::class.java)[0]
            //打开相机
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mainHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        screenShootNeedStop = true
        return false
    }

    // 这个方法要注意一下，因为每有一帧画面，都会回调一次此方法
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        surfaceView!!.drawRect()
        screenShootNeedStop = false
    }


    private val mCameraDeviceStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            try {
                screenShootNeedStop = false
                mCameraDevice = camera
                startPreview(camera)
                Timer().schedule(object : TimerTask() {

                    val regex = "^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\\d{8}$"

                    val pattern = Pattern.compile(regex)

                    override fun run() {
                        if (screenShootNeedStop) {
                            return
                        }
                        val bitmap = convertToBMW(Bitmap.createBitmap(mTextureView!!.bitmap, mTextureView!!.width / 2 - 300,
                                mTextureView!!.height / 2 - 55, 600, 110),600, 110, 125)
                        val phoneNumber = numberRecognize!!.recognize(bitmap)
                        for (line in phoneNumber.split("\n")) {
                            val strings = phoneNumber.split(" ")
                            for (subString in strings) {
                                if (pattern.matcher(subString).matches()) {
                                    val message = Message()
                                    message.what = 1
                                    message.obj = subString
                                    myHandler.sendMessage(message)
                                    currentRecognizeString = subString
                                    return
                                }
                            }
                        }
                    }
                }, Date(), 1)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Toast.makeText(this@CameraActivity, "摄像头开启失败", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * 开始预览
     */
    private fun startPreview(camera: CameraDevice) {
        val texture = mTextureView!!.surfaceTexture

        texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
        val surface = Surface(texture)

        try {
            // 设置捕获请求为预览，这里还有拍照啊，录像等
            mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            return
        }

        val mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1);

        camera.createCaptureSession(Arrays.asList(surface, mImageReader.surface
        ), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                if (null == mCameraDevice) return
                // 当摄像头已经准备好时，开始显示预览
                mCameraCaptureSession = cameraCaptureSession
                try {
                    // 自动对焦
                    mPreviewBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    // 打开闪光灯
                    mPreviewBuilder!!.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                    // 显示预览
                    mPreviewBuilder!!.addTarget(surface)
                    mCameraCaptureSession!!.setRepeatingRequest(mPreviewBuilder!!.build(), null, childHandler)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                Toast.makeText(this@CameraActivity, "配置失败", Toast.LENGTH_SHORT).show()
            }
        }, childHandler)
    }


    override fun onClick(v: View) {
        when(v.id) {
            R.id.confirm -> {
                savedString.add(currentRecognizeString)
            }
        }
    }


    override fun onBackPressed() {
        val it = intent
        val bd = Bundle()
        bd.putStringArrayList("phoneNumber", savedString)
        it.putExtras(bd)
        setResult(0x01, it)
        finish()
    }

    fun convertToBMW(bmp: Bitmap, w: Int, h: Int, tmp: Int): Bitmap {
        val width = bmp.width // 获取位图的宽
        val height = bmp.height // 获取位图的高
        val pixels = IntArray(width * height) // 通过位图的大小创建像素点数组
        // 设定二值化的域值，默认值为100
        //tmp = 180;
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)
        var alpha = 0xFF shl 24
        for (i in 0 until height) {
            for (j in 0 until width) {
                val grey = pixels[width * i + j]
                // 分离三原色
                alpha = grey and -0x1000000 shr 24
                var red = grey and 0x00FF0000 shr 16
                var green = grey and 0x0000FF00 shr 8
                var blue = grey and 0x000000FF
                if (red > tmp) {
                    red = 255
                } else {
                    red = 0
                }
                if (blue > tmp) {
                    blue = 255
                } else {
                    blue = 0
                }
                if (green > tmp) {
                    green = 255
                } else {
                    green = 0
                }
                pixels[width * i + j] = (alpha shl 24 or (red shl 16) or (green shl 8)
                        or blue)
                if (pixels[width * i + j] == -1) {
                    pixels[width * i + j] = -1
                } else {
                    pixels[width * i + j] = -16777216
                }
            }
        }
        // 新建图片
        val newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // 设置图片数据
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return ThumbnailUtils.extractThumbnail(newBmp, w, h)
    }


}