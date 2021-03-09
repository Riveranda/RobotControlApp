package com.bender.robotics

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var client: Client? =
        null // This client object will be responsible for connecting to your robot via server sockets.
    // it is defined below as an internal class.

    // CHANGE PORT AND IPV4 HERE
    companion object {
        const val PORT = 12523
        var IPV4 = "loc"
    }

    private val prefName = "IP_ADDRESS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onClickListeners()
    }

    /**
     * Button click listeners
     */
    private fun onClickListeners() {
        findViewById<Button>(R.id.forward).setOnClickListener {
            client?.write("1")
        }
        findViewById<Button>(R.id.backward).setOnClickListener {
            client?.write("2")
        }
        findViewById<Button>(R.id.right).setOnClickListener {
            client?.write("3")
        }
        findViewById<Button>(R.id.left).setOnClickListener {
            client?.write("4")
        }
        val ipText = findViewById<EditText>(R.id.iptext)
        val sharedPref: SharedPreferences = getSharedPreferences(prefName, 0)
        if (!sharedPref.getString(prefName, "").equals("")) {
            ipText.setText(sharedPref.getString(prefName, ""))
        } else {
            ipText.setText(IPV4)
        }
        findViewById<Button>(R.id.connect).setOnClickListener {
            if (ipText.text.isBlank()) {
                Toast.makeText(this, "PLease enter an IPV4 Address", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    IPV4 = ipText.text.toString().trim()
                    if (client == null) {
                        client = Client(this)
                        client?.initiate()
                    } else {
                        client?.initiate()
                    }
                    val editor = sharedPref.edit()
                    editor.putString(prefName, ipText.text.toString())
                    editor.apply()
                } catch (ignored: Exception) {
                    println("EXCEPTION")
                }
            }
        }
        findViewById<Button>(R.id.disconnect).setOnClickListener {
            try {
                client?.disconnect(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Called when the activity is destroyed, aka you close the app. Will attempt to automatically disconnect the socket.
     */
    override fun onDestroy() {
        client?.disconnect(false)
        super.onDestroy()
    }

    private var reconnect = false

    /**
     * If the app is paused, we also need to disconnect
     */
    override fun onPause() {
        client?.disconnect(false)
        reconnect = true
        super.onPause()
    }

    override fun onResume() {
        if (reconnect) {
            client?.initiate()
            reconnect = false
        }
        super.onResume()
    }

    /**
     * This class handles the server sockets and messaging which communicate with your robot via wifi. We can definitely add a bluetooth option later,
     *  but that will add several hundred more lines of code. This is the simple way to do it, and requires you to both be on the same wifi network.
     */
    class Client(private var context: Context?) {
        @Volatile
        private var client: Socket? = null // Server socket.
        private val handler: Handler =
            Handler(Looper.getMainLooper()) //You must use a handler to perform any action on the UI thread from a separate thread
        private val executor: ExecutorService = Executors.newSingleThreadExecutor() //multithreading

        // this is used to create a Toast(Popup) from a separate thread
        private var bool = false

        /*
         * This function will write a message to the socket. It is synchronized to prevent an overflow error.
         */
        @Synchronized
        fun write(message: String) {
            executor.execute {
                try {
                    if (client != null)
                        client!!.outputStream.write(message.toByteArray())
                    else initiate()
                } catch (e: Exception) { //In case of an error we will disconnect from the socket.
                    disconnect(true)
                }
            }
        }

        @Synchronized
        fun initiate() {
            println("Initiate")
            val executor = Executors.newSingleThreadExecutor() //more multithreading
            executor.execute { //from this point on, we will be on a separate thread
                while ((client == null || !client?.isConnected!!) && !bool) { //Loop until we are connected.
                    try {
                        println("Attempting to create a socket")
                        client = Socket(IPV4, PORT) //attempt to form a socket connection
                    } catch (ignored: Exception) {
                        println("Retrying in 5s")
                        handler.post {
                            Toast.makeText(context, "Retrying in 5s", Toast.LENGTH_SHORT).show()
                        }
                        Thread.sleep(5000)
                        continue
                    }
                    //If we reach this point, we have a valid socket created.
                    println("Connected")
                    handler.post {
                        Toast.makeText(context, "Server Connected", Toast.LENGTH_SHORT)
                            .show() //notify the user the socket is functional
                    }
                }
                bool = false
            }
        }


        @Synchronized
        fun disconnect(reconnect: Boolean) {
            try {
                bool = true
                client?.close()
                client = null
                if (reconnect) {
                    initiate()
                }
                handler.post {
                    Toast.makeText(context, "Server Disconnected", Toast.LENGTH_SHORT).show()
                }
                bool = false
                println("Disconnected")
            } catch (e: Exception) {
            }
        }
    }

}