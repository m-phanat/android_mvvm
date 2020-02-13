package com.panat.mvvm.retrofit.view

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.panat.mvvm.retrofit.R
import com.panat.mvvm.retrofit.databinding.ActivitySocketBinding
import com.panat.mvvm.retrofit.service.AckWithTimeOut
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject

class SocketActivity : AppCompatActivity() {


    private lateinit var socket: Socket
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socket)
        val binding =
            DataBindingUtil.setContentView<ActivitySocketBinding>(this, R.layout.activity_socket)

        openSocket()
        initSocket()

        binding.send.setOnClickListener {
            if (binding.message.text.toString().isNotEmpty()) {

                val obj = JSONObject()
                try {
                    obj.put("name", binding.message.text.toString())
                    obj.put("age", 20)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                val timeOut = 10000L
                socket.emit("news-ack", obj, object : AckWithTimeOut(timeOut) {
                    override fun call(vararg args: Any?) {
                        if (args[0].toString().equals("No Ack", true)) {
                            Log.d("ack_socket", "AckWithTimeOut : " + args[0].toString())
                        } else if (args[0].toString().equals("woot", true)) {
                            cancelTimer() //cancel timer if emit ACK return true
                            Log.d("ack_socket", "AckWithTimeOut : " + args[0].toString())
                        }
                    }
                })

                socket.emit("news", obj)
                binding.message.setText("")
            }
        }


        socket.on("paper-ack") { args ->
            runOnUiThread {
                println("ack_data paper-news  ${args[0]}")
            }
        }
    }

    private fun openSocket() {
        try {
            socket = IO.socket("http://10.168.55.101:8080/")
        } catch (e: Exception) {
            println(e.message)
            e.printStackTrace()
        }

    }

    private fun initSocket() {
        socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError)
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError)
        socket.on(Socket.EVENT_RECONNECT_FAILED, onConnectError)
        socket.on(Socket.EVENT_DISCONNECT, onConnectError)
        socket.on(Socket.EVENT_RECONNECT_ERROR, onConnectError)
        socket.on(Socket.EVENT_CONNECT) { }
        socket.on(Socket.EVENT_RECONNECT, onConnectError)
        socket.connect()
    }

    private var onConnectError = Emitter.Listener { args ->
        Log.e("onConnectError", "Exception " + args[0])
    }
}