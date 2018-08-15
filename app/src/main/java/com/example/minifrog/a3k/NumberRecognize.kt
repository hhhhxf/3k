package com.example.minifrog.a3k

import android.os.Environment
import android.content.Context
import android.graphics.Bitmap
import java.io.*
import com.googlecode.tesseract.android.TessBaseAPI


/**
 * Created by mini-frog on 2018/4/9.
 *
 */


class NumberRecognize(private val context: Context) {

    private val sdCardPath = Environment.getExternalStorageDirectory().absolutePath + File.separator

    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private val tessDirectory = sdCardPath + File.separator + "tessdata"

    /**
     * TessBaseAPI初始化测第二个参数，就是识别库的名字不要后缀名。
     */
    private val trainedFileNameWithoutSuffix = "num"

    /**
     * assets中的文件名
     */
    private val trainedFileCompleteName = "$trainedFileNameWithoutSuffix.traineddata"

    /**
     * 保存到SD卡中的完整文件名
     */
    private val trainedFilePathInSDCard = tessDirectory + File.separator + trainedFileCompleteName

    private val tessBaseAPI = TessBaseAPI()

    init {
        copyTessToSDCard()
        tessBaseAPI.init(sdCardPath, trainedFileNameWithoutSuffix)
    }

    /**
     * 拷贝训练文件到sd卡
     */
    private fun copyTessToSDCard() {
        val file = File(trainedFilePathInSDCard)
        if (file.exists()) {
            file.delete()
        } else {
            val fileDir = File(file.parent)
            if (!fileDir.exists()) {
                fileDir.mkdirs()
            }
            try {
                fileDir.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        var inStream: InputStream? = null
        var outStream: OutputStream? = null
        try {
            inStream = context.assets.open(trainedFileCompleteName)
            val tessFile = File(trainedFilePathInSDCard)
            outStream = FileOutputStream(tessFile)
            val bytes = ByteArray(2048)
            var len: Int
            while (true) {
                len = inStream.read(bytes)
                if (len == -1)
                    break
                outStream.write(bytes, 0, len)
            }
            outStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inStream?.close()
                outStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun recognize(bitmap: Bitmap): String {
        tessBaseAPI.setImage(bitmap)
        tessBaseAPI.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_TRUE);
        return tessBaseAPI.utF8Text
    }

}