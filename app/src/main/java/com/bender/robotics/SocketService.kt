package com.bender.robotics

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SocketService : Service() {

    private val binder = LocalBinder()
    private val client = Client(this)

    inner class LocalBinder : Binder() {
        fun getService(): SocketService = this@SocketService
    }

    override fun onBind(intent: Intent?): IBinder {
        if(client.client == null){
            client.initiate()
        }
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect(false)
        return super.onUnbind(intent)
    }

    fun disconnect(bool: Boolean) {
        client.disconnect(bool)
    }

    fun initiate() {
        try {
            if (client.client == null) {
                client.initiate()
            }
        } catch (e: Exception) {

        }
    }

    fun write(s: String) {
        client.write(s)
    }

    /**
     * This class handles the server sockets and messaging which communicate with your robot via wifi. We can definitely add a bluetooth option later,
     *  but that will add several hundred more lines of code. This is the simple way to do it, and requires you to both be on the same wifi network.
     */
    inner class Client(private var context: Context?) {
        @Volatile
        var client: Socket? = null // Server socket.
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
                        client = Socket(
                            MainActivity.IPV4,
                            MainActivity.PORT
                        ) //attempt to form a socket connection
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