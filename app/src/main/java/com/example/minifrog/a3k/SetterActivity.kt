package com.example.minifrog.a3k

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.azhon.suspensionfab.ExpandOrientation
import com.azhon.suspensionfab.FabAttributes
import com.azhon.suspensionfab.OnFabClickListener
import com.azhon.suspensionfab.SuspensionFab
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset

/**
 * Created by mini-frog on 2018/4/13.
 *
 * setter activity
 */

class SetterActivity : Activity() {

    private var myRecyclerView: RecyclerView? = null

    private var myRecyclerViewAdapter: RecyclerViewAdapter? = null

    private var suspensionFab: SuspensionFab? = null

    private var networkConnection: NetworkConnection? = null

    private var bluetoothUtility: BlueToothUtility? = null

    private var setterViewHandler: SetterViewHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setter)

        initView()

        setterViewHandler = SetterViewHandler(this)
        networkConnection = NetworkConnection(setterViewHandler!!)
        bluetoothUtility = BlueToothUtility(this, setterViewHandler!!)
        val userInfo = intent.getStringArrayListExtra("user_info")
        networkConnection!!.phone = userInfo[0]
        networkConnection!!.password = userInfo[1]
        networkConnection!!.cookie = intent.getStringArrayListExtra("cookie")
        if (intent.getStringExtra("is_close_door") == "true") {
            messageLastTimeNotCloseDoor()
        }
    }


    override fun onPause() {
        super.onPause()
        myRecyclerViewAdapter!!.storeAll()
    }


    override fun onDestroy() {
        super.onDestroy()
        bluetoothUtility?.stopBlueTooth()
        logout()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x00) {
            val bd = data?.getStringArrayListExtra("phoneNumber")
            bd?.let {
                for(string in bd) {
                    myRecyclerViewAdapter!!.add(DataBean(DataBean.DATA_TYPE_PARENT, string), myRecyclerViewAdapter!!.itemCount)
                }
            }
        }
    }


    private fun initView() {
        myRecyclerView = findViewById(R.id.my_recycler_view)
        myRecyclerViewAdapter = RecyclerViewAdapter()
        myRecyclerView!!.layoutManager = LinearLayoutManager(this)
        myRecyclerView!!.adapter = myRecyclerViewAdapter
        myRecyclerView!!.itemAnimator = DefaultItemAnimator()
        myRecyclerView!!.addItemDecoration(MyDividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        myRecyclerViewAdapter!!.recreateFromStorage()
        val commonAddButton = FabAttributes.Builder()
                .setBackgroundTint(Color.parseColor("#57b7a6"))
                .setSrc(getDrawable(R.drawable.take_photo))
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .setPressedTranslationZ(10)
                .setTag(1)
                .build()
        val cameraAddButton = FabAttributes.Builder()
                .setBackgroundTint(Color.parseColor("#8289a7"))
                .setSrc(getDrawable(R.drawable.add))
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .setPressedTranslationZ(10)
                .setTag(2)
                .build()
        val deleteAllButton = FabAttributes.Builder()
                .setBackgroundTint(Color.parseColor("#db4438"))
                .setSrc(getDrawable(R.drawable.delete))
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .setPressedTranslationZ(10)
                .setTag(3)
                .build()
        suspensionFab = findViewById(R.id.floating_menu)
        suspensionFab!!.setOrientation(ExpandOrientation.FAB_LEFT)
        suspensionFab!!.addFab(cameraAddButton, commonAddButton, deleteAllButton)
        suspensionFab!!.setFabClickListener(MenuClickListener())
    }


    private fun changeToRecognize() {
        val getterActivity = Intent(this, CameraActivity::class.java)
        getterActivity.putStringArrayListExtra("user_info", networkConnection!!.getUserInfo())
        getterActivity.putStringArrayListExtra("cookie", networkConnection!!.getCookieArrayList())
        startActivityForResult(getterActivity, 0x00)
    }


    internal class SetterViewHandler(private val context: Context) : BaseHandler(context) {

        companion object {
            const val PHONE_NUMBER_EDITABLE_MESSAGE = 5
            const val PHONE_NUMBER_UNEDITABLE_MESSAGE = 6
            const val SET_FOOD_BUTTON_LOCK_MESSAGE = 7
            const val SET_FOOD_BUTTON_UNLOCK_MESSAGE = 8
        }

        override fun handleMessage(msg: Message?) {
            msg?.let {
                super.handleMessage(msg)
                when (msg.what) {
                    PHONE_NUMBER_EDITABLE_MESSAGE -> {
                        (msg.obj as RelativeLayout).findViewById<EditText>(R.id.phone_number).isEnabled = true
                    }
                    PHONE_NUMBER_UNEDITABLE_MESSAGE -> {
                        (msg.obj as RelativeLayout).findViewById<EditText>(R.id.phone_number).isEnabled = false
                    }
                    SET_FOOD_BUTTON_LOCK_MESSAGE -> {
                        val hot = (msg.obj as LinearLayout).findViewById<Button>(R.id.set_food_hot)
                        val cold = (msg.obj as LinearLayout).findViewById<Button>(R.id.set_food_cold)
                        val phone = (msg.obj as LinearLayout).findViewById<Button>(R.id.call_phone)
                        cold.isClickable = false
                        hot.isClickable = false
                        phone.isClickable = false
                        cold.background = context.getDrawable(R.drawable.button_untouchable_background)
                        hot.background = context.getDrawable(R.drawable.button_untouchable_background)
                        phone.background = context.getDrawable(R.drawable.button_untouchable_background)
                    }
                    SET_FOOD_BUTTON_UNLOCK_MESSAGE -> {
                        val hot = (msg.obj as LinearLayout).findViewById<Button>(R.id.set_food_hot)
                        val cold = (msg.obj as LinearLayout).findViewById<Button>(R.id.set_food_cold)
                        val phone = (msg.obj as LinearLayout).findViewById<Button>(R.id.call_phone)
                        cold.isClickable = true
                        hot.isClickable = true
                        phone.isClickable = true
                        cold.background = context.getDrawable(R.drawable.button_touchable_background)
                        hot.background = context.getDrawable(R.drawable.button_touchable_background)
                        phone.background = context.getDrawable(R.drawable.button_touchable_background)
                    }
                }
            }
        }
    }


    private inner class MenuClickListener : OnFabClickListener {

        override fun onFabClick(fab: FloatingActionButton?, tag: Any?) {
            tag?.let {
                when (it) {
                    1 -> {
                        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            messagePermissionFail()
                        } else {
                            changeToRecognize()
                        }
                    }
                    2 -> {
                        val bean = DataBean(DataBean.DATA_TYPE_PARENT, "")
                        myRecyclerViewAdapter!!.add(bean, myRecyclerViewAdapter!!.itemCount)
                    }
                    3 -> {
                        myRecyclerViewAdapter!!.removeAll()
                    }
                }
            }
        }
    }


    private inner class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(this@SetterActivity)

        private val myDataBean = ArrayList<DataBean>()

        private var idCount = 0

        private var itemClickListener = object : ItemClickListener {

            override fun onExpandChildren(bean: DataBean) {
                val position = getCurrentPosition(bean.dataBeanId)
                val child = getChildDataBean(bean)
                add(child, position + 1)
            }

            override fun onHideChildren(bean: DataBean) {
                val position = getCurrentPosition(bean.dataBeanId)
                val children = bean.childDataBean
                children.let {
                    remove(position + 1)
                }
            }
        }


        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                DataBean.DATA_TYPE_CHILD -> {
                    ChildViewHolder(inflater.inflate(R.layout.child_recycler_item, parent, false))
                }
                DataBean.DATA_TYPE_PARENT -> {
                    ParentViewHolder(inflater.inflate(R.layout.recycler_item, parent, false))
                }
                else -> {
                    ParentViewHolder(inflater.inflate(R.layout.recycler_item, parent, false))
                }
            }
        }


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            when (myDataBean[position].dataType) {
                DataBean.DATA_TYPE_CHILD -> {
                    (holder as ChildViewHolder).bindView(holder, myDataBean[position], position)
                }
                DataBean.DATA_TYPE_PARENT -> {
                    (holder as ParentViewHolder).bindView(myDataBean[position], position)
                }
            }
        }


        override fun getItemCount(): Int {
            return myDataBean.count()
        }

        override fun getItemViewType(position: Int): Int {
            return myDataBean[position].dataType
        }

        private fun getCurrentPosition(uuid: Int): Int {
            for (i in 0 until myDataBean.count()) {
                if (uuid == myDataBean[i].dataBeanId) {
                    return i
                }
            }
            return -1
        }

        private fun getChildDataBean(parent: DataBean): DataBean {
            val child = DataBean(DataBean.DATA_TYPE_CHILD, "")
            child.isExpand = false
            child.childDataBean = null
            child.parentDataBean = parent
            child.phoneNumberEditable = false
            return child
        }


        fun add(data: DataBean, position: Int) {
            idCount++
            data.dataBeanId = idCount - 1
            myDataBean.add(position, data)
            notifyItemInserted(position)
        }


        fun remove(position: Int) {
            myDataBean.removeAt(position)
            notifyItemRemoved(position)
        }


        fun storeAll() {
            var phoneNumberFile: FileOutputStream? = null
            try {
                phoneNumberFile = openFileOutput("phoneNumber.txt", Context.MODE_PRIVATE)
                var phoneNumbers = ""
                for (dataBean in myDataBean) {
                    if (dataBean.contents == "") continue
                    phoneNumbers += "${dataBean.contents}\n"
                }
                phoneNumberFile.write(phoneNumbers.toByteArray())
            } catch (error: IOException) {
                error.printStackTrace()
            } finally {
                phoneNumberFile?.close()
            }
        }


        fun recreateFromStorage() {
            try {
                openFileInput("phoneNumber.txt").close()
            } catch (error: IOException) {
                return
            }
            var phoneNumberFile: FileInputStream? = null
            try {
                phoneNumberFile = openFileInput("phoneNumber.txt")
                val buffer = ByteArray(phoneNumberFile.available())
                phoneNumberFile.read(buffer)
                var count = 0
                for (line in String(buffer, Charset.forName("ascii")).split("\n")) {
                    if (line == "") continue
                    add(DataBean(DataBean.DATA_TYPE_PARENT, line), count)
                    count++
                }
            } catch (error: IOException) {
                error.printStackTrace()
            } finally {
                phoneNumberFile?.close()
            }
        }


        fun removeAll() {
            val total = itemCount
            var count = 0
            while (true) {
                if (count == total)
                    break
                if (myDataBean[0].isExpand) {
                    remove(1)
                    remove(0)
                    count += 2
                    continue
                }
                remove(0)
                count++
            }
            idCount = 0
        }


        internal inner class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            var phoneNumber: EditText = itemView.findViewById(R.id.phone_number)

            fun bindView(dataBean: DataBean, position: Int) {
                val parentClickListener = ParentClickListener(dataBean, itemClickListener)
                val textWatcher = SetterTextWatcher(dataBean, this)
                phoneNumber.addTextChangedListener(textWatcher)
                phoneNumber.setText(myDataBean[position].contents.toCharArray(), 0, myDataBean[position].contents.length)
                phoneNumber.isEnabled = when (phoneNumber.text.length == 11) {
                    true -> false
                    false -> true
                }
                itemView.findViewById<Button>(R.id.delete)?.setOnClickListener(parentClickListener)
                itemView.findViewById<Button>(R.id.edit)?.setOnClickListener(parentClickListener)
                itemView.setOnClickListener(parentClickListener)
            }

            fun onPhoneNumberValid(dataBean: DataBean) {
                dataBean.childDataBean?.let {
                    val childViewHolder = dataBean.childViewHolder as ChildViewHolder
                    childViewHolder.onPhoneNumberValid()
                }
            }

            fun onPhoneNumberInvalid(dataBean: DataBean) {
                dataBean.childDataBean?.let {
                    val childViewHolder = dataBean.childViewHolder as ChildViewHolder
                    childViewHolder.onPhoneNumberInvalid()
                }
            }
        }


        internal inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            var callButton: Button = itemView.findViewById(R.id.call_phone)

            var setFoodHotButton: Button = itemView.findViewById(R.id.set_food_hot)

            var setFoodColdButton: Button = itemView.findViewById(R.id.set_food_cold)

            var item: LinearLayout = (itemView as LinearLayout).getChildAt(0) as LinearLayout

            fun bindView(holder: ChildViewHolder, dataBean: DataBean, position: Int) {
                val childOnClickListener = ChildClickListener(dataBean)
                dataBean.parentDataBean!!.childViewHolder = holder
                dataBean.parentDataBean!!.childDataBean = dataBean
                callButton.setOnClickListener(childOnClickListener)
                setFoodHotButton.setOnClickListener(childOnClickListener)
                setFoodColdButton.setOnClickListener(childOnClickListener)
                if (dataBean.parentDataBean!!.phoneNumberValid) {
                    onPhoneNumberValid()
                } else {
                    onPhoneNumberInvalid()
                }
            }

            fun onPhoneNumberValid() {
                messageSetFoodButtonLock(false, item)
            }

            fun onPhoneNumberInvalid() {
                messageSetFoodButtonLock(true, item)
            }
        }


        internal inner class ChildClickListener(private val dataBean: DataBean) : View.OnClickListener {

            override fun onClick(view: View) {
                when (view.id) {
                    R.id.set_food_hot -> {
                        this@SetterActivity.setFoodBoxInfo(dataBean.parentDataBean!!.contents, view.parent as LinearLayout, true)
                    }
                    R.id.set_food_cold -> {
                        this@SetterActivity.setFoodBoxInfo(dataBean.parentDataBean!!.contents, view.parent as LinearLayout, false)
                    }
                    R.id.call_phone -> {
                        val uri = Uri.parse ("tel:" + dataBean.parentDataBean!!.contents)
                        val intent = Intent(Intent.ACTION_DIAL, uri)
                        this@SetterActivity.startActivity(intent)
                    }
                }
            }
        }

        internal inner class ParentClickListener(private var dataBean: DataBean,
                                                 private var itemClickListener: ItemClickListener) : View.OnClickListener {
            override fun onClick(view: View?) {
                view?.let {
                    when (it.id) {
                        R.id.recycler_item -> {
                            if (dataBean.isExpand) {
                                itemClickListener.onHideChildren(dataBean)
                                dataBean.isExpand = false
                            } else {
                                itemClickListener.onExpandChildren(dataBean)
                                dataBean.isExpand = true
                            }
                        }
                        R.id.edit -> {
                            when (dataBean.phoneNumberEditable) {
                                false -> {
                                    dataBean.phoneNumberEditable = true
                                    phoneNumberEditable(view.parent as RelativeLayout)
                                }
                                true -> {
                                    dataBean.phoneNumberEditable = false
                                    phoneNumberUneditable(view.parent as RelativeLayout)
                                }
                            }
                        }
                        R.id.delete -> {
                            if (dataBean.isExpand) {
                                remove(getCurrentPosition(dataBean.dataBeanId) + 1)
                            }
                            remove(getCurrentPosition(dataBean.dataBeanId))
                        }
                        R.id.phone_number -> {

                        }
                    }
                }
            }
        }

        internal inner class SetterTextWatcher(private var dataBean: DataBean,
                                               private var viewHolder: ParentViewHolder) : TextWatcher {

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                dataBean.contents = s.toString()
                if (s.length == 11) {
                    dataBean.phoneNumberValid = true
                    viewHolder.onPhoneNumberValid(dataBean)
                } else {
                    dataBean.phoneNumberValid = false
                    viewHolder.onPhoneNumberInvalid(dataBean)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        }
    }


    private fun logout() {
        networkConnection!!.cleanCookie()
        this.openFileOutput("register.txt", Context.MODE_PRIVATE).close()
    }


    fun setFoodBoxInfo(phone: String, recyclerItem: LinearLayout, warm: Boolean) {
        val jsonData = JSONObject()
        jsonData.put("phone", phone)
        when (warm) {
            true -> jsonData.put("warm", "true")
            false -> jsonData.put("warm", "false")
        }

        Thread(Runnable {
            try {
                messageSetFoodButtonLock(true, recyclerItem)
                val receiveData = networkConnection!!.postRequest("food/set", jsonData.toString())
                receiveData?.let {
                    val receiveJson = JSONObject(it)
                    if (receiveJson.get("status") == "true" && receiveJson.get("code") == "0") {
                        messageSetFoodBoxSuccess()

                        bluetoothUtility!!.setBlueTooth(receiveJson.get("box_name").toString(), receiveJson.get("box_password").toString())
                        if (!bluetoothUtility!!.setterOpenBoxService(phone, warm)) {
                            setFoodFail()
                            messageSetFoodButtonLock(false, recyclerItem)
                            return@Runnable
                        }

                        setFoodSuccess(phone, warm)

                    } else {
                        messageSetFoodBoxFail((receiveJson.get("code") as String).toInt())
                    }
                    messageSetFoodButtonLock(false, recyclerItem)
                    return@Runnable
                }
                messageSetFoodButtonLock(false, recyclerItem)
            } catch (e: Exception) {
                messageSetFoodBoxFail(Int.MAX_VALUE)
                e.printStackTrace()
                messageSetFoodButtonLock(false, recyclerItem)
            }
        }).start()
    }


    private fun setFoodSuccess(phone: String, warm: Boolean) {
        val jsonData = JSONObject()
        jsonData.put("phone", phone)
        when (warm) {
            true -> jsonData.put("warm", "true")
            false -> jsonData.put("warm", "false")
        }

        Thread(Runnable {
            try {
                val receiveData = networkConnection!!.postRequest("food/setSuccess", jsonData.toString())
                receiveData?.let {
                    val receiveJson = JSONObject(it)
                    if (receiveJson.get("status") == "true" && receiveJson.get("code") == "0") {
                        //messageSetFoodSuccess()
                    } else {
                        messageSetFoodFail((receiveJson.get("code") as String).toInt())
                    }
                    return@Runnable
                }
            } catch (e: Exception) {
                messageSetFoodBoxFail(Int.MAX_VALUE)
                e.printStackTrace()
            }
        }).start()
    }


    private fun setFoodFail() {
        Thread(Runnable {
            try {
                val receiveData = networkConnection!!.postRequest("food/setFail", "")
                receiveData?.let {
                    val receiveJson = JSONObject(it)
                    if (receiveJson.get("status") == "true" && receiveJson.get("code") == "0") {
                        //messageSetFoodSuccess()
                    } else {
                        messageSetFoodFail((receiveJson.get("code") as String).toInt())
                    }
                    return@Runnable
                }
            } catch (e: Exception) {
                messageSetFoodBoxFail(Int.MAX_VALUE)
                e.printStackTrace()
            }
        }).start()
    }


    private fun messageSetFoodSuccess() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "服务器同步成功"
        setterViewHandler!!.sendMessage(message)
    }

    private fun messageSetFoodFail(code: Int) {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "服务器同步失败" + when (code) {
            1 -> "未以快送员方式登陆"
            2 -> "参数错误"
            3 -> "该用户不存在"
            4 -> "该用户存在未取出的食物"
            5 -> "空箱子不足"
            else -> "未知的错误"
        }
        setterViewHandler!!.sendMessage(message)
    }

    private fun messageSetFoodBoxFail(code: Int) {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "箱门信息获取失败" + when (code) {
            1 -> "未以快送员方式登陆"
            2 -> "参数错误"
            3 -> "该用户不存在"
            4 -> "该用户存在未取出的食物"
            5 -> "空箱子不足"
            else -> "未知的错误"
        }
        setterViewHandler!!.sendMessage(message)
    }

    private fun messageSetFoodBoxSuccess() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "箱门信息获取成功"
        setterViewHandler!!.sendMessage(message)
    }

    private fun messageSetFoodButtonLock(lock: Boolean, view: View) {
        val message = Message()
        message.what = when (lock) {
            true -> SetterViewHandler.SET_FOOD_BUTTON_LOCK_MESSAGE
            false -> SetterViewHandler.SET_FOOD_BUTTON_UNLOCK_MESSAGE
        }
        message.obj = view
        setterViewHandler!!.sendMessage(message)
    }

    private fun phoneNumberEditable(view: View) {
        val message = Message()
        message.what = SetterViewHandler.PHONE_NUMBER_EDITABLE_MESSAGE
        message.obj = view
        setterViewHandler!!.sendMessage(message)
    }

    private fun phoneNumberUneditable(view: View) {
        val message = Message()
        message.what = SetterViewHandler.PHONE_NUMBER_UNEDITABLE_MESSAGE
        message.obj = view
        setterViewHandler!!.sendMessage(message)
    }


    private fun messagePermissionFail() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "权限不足，请手动开启权限后重试"
        setterViewHandler!!.sendMessage(message)
    }


    private fun messageLastTimeNotCloseDoor() {
        val message = Message()
        message.what = BaseHandler.REMIND_MESSAGE
        message.obj = "上一次使用外卖箱没有关闭箱门"
        setterViewHandler!!.sendMessage(message)
    }


}