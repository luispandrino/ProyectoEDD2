package com.example.messengerapp


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.Toast
import com.example.messengerapp.R.id.Chat
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.PrivateChannelEventListener
import com.pusher.client.util.HttpAuthorizer
import kotlinx.android.synthetic.main.activity_chat_room.*
import kotlinx.android.synthetic.main.activity_chat_room.view.*
import kotlinx.android.synthetic.main.chat_item.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*
import java.util.jar.Manifest


class ChatRoom : AppCompatActivity() {



  companion object {
    const val EXTRA_ID = "id"
    const val EXTRA_NAME = "name"
    const val EXTRA_COUNT = "numb"
  }

  private lateinit var contactName: String
  private lateinit var contactId: String
  private var contactNumb: Int = -1
  lateinit var nameOfChannel: String
  private  var lv: ListView? = null


  val mAdapter = ChatRoomAdapter(ArrayList())


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_chat_room)
    fetchExtras()

    subscribeToChannel()
    setupClickListener()

    val listView = findViewById<ListView>(R.id.Chat)



  }




  private fun fetchExtras() {
    this.contactName = intent.extras.getString(ChatRoom.EXTRA_NAME)
    this.contactId = intent.extras.getString(ChatRoom.EXTRA_ID)
    this.contactNumb = intent.extras.getInt(ChatRoom.EXTRA_COUNT)
  }





  private fun subscribeToChannel() {
    val authorizer = HttpAuthorizer("http://192.168.0.26:5000/pusher/auth/private")
    val options = PusherOptions().setAuthorizer(authorizer)
    options.setCluster("PUSHER_APP_CLUSTER")

    val pusher = Pusher("PUSHER_APP_KEY", options)
    pusher.connect()

    nameOfChannel = if (Singleton.getInstance().currentUser.count > contactNumb) {
      "private-" + Singleton.getInstance().currentUser.id + "-" + contactId
    } else {
      "private-" + contactId + "-" + Singleton.getInstance().currentUser.id
    }

    Log.i("ChatRoom", nameOfChannel)

    pusher.subscribePrivate(nameOfChannel, object : PrivateChannelEventListener {
      override fun onEvent(channelName: String?, eventName: String?, data: String?) {

        val jsonObject = JSONObject(data)
        val messageModel = MessageModel(
            jsonObject.getString("message"),
            jsonObject.getString("sender_id"))

        runOnUiThread {
          mAdapter.add(messageModel)
        }

      }

      override fun onAuthenticationFailure(p0: String?, p1: Exception?) {
        Log.e("ChatRoom", p1!!.localizedMessage)
      }

      override fun onSubscriptionSucceeded(p0: String?) {
        Log.i("ChatRoom", "Successful subscription")
      }

    }, "new-message")

  }


  private fun setupClickListener() {

    sendButton.setOnClickListener{
      if (editText.text.isNotEmpty()){
        val jsonObject = JSONObject()
        jsonObject.put("message",editText.text.toString())
        jsonObject.put("channel_name",nameOfChannel)
        jsonObject.put("sender_id",Singleton.getInstance().currentUser.id)
        val jsonBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
            jsonObject.toString())



        RetrofitInstance.retrofit.sendMessage(jsonBody).enqueue(object: Callback<String>{
          override fun onFailure(call: Call<String>?, t: Throwable?) {
            Log.e("ChatRoom",t!!.localizedMessage)

          }

          override fun onResponse(call: Call<String>?, response: Response<String>?) {
            Log.e("ChatRoom",response!!.body())
          }

        })
        editText.text.clear()
        hideKeyBoard()
      }

    }

    File.setOnClickListener{
      val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
      startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)

    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if(requestCode == 111 && resultCode == Activity.RESULT_OK){
      val selectedFile = data?.data
      var path = selectedFile!!.path
      var lz = LZW()
      var cadenaImagen = lz.comprimirImagen(path)

      val jsonObject = JSONObject()
      jsonObject.put("image",cadenaImagen)
      jsonObject.put("channel_name",nameOfChannel)
      jsonObject.put("sender_id",Singleton.getInstance().currentUser.id)
      val jsonBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
              jsonObject.toString())

    }
  }




  private fun hideKeyBoard() {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = currentFocus
    if (view == null) {
      view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
  }


}
