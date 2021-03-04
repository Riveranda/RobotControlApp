package com.example.laceysapp

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.Socket
import java.util.concurrent.Executors
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {
    private var client: Client? = null

    // CHANGE PORT AND IPV4 HERE
    companion object {
        const val PORT = 9000
        const val IPV4 = "192.168.10.201"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }

    override fun onStart() {
        super.onStart()
        try {
            client = Client(this)
            client?.initiate()
        } catch (ignored: Exception) {
            println("EXCEPTION")
        }
    }

    override fun onDestroy() {
        client?.disconnect()
        super.onDestroy()
    }


    class Client(private var context: Context?) {
        @Volatile
        private var client: Socket? = null
        private val handler: Handler = Handler()

        @Synchronized
        fun write(message: String) {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                try {
                    if (client != null)
                        client!!.outputStream.write(message.toByteArray())
                    else initiate()
                } catch (e: Exception) {
                    disconnect()
                }
            }
        }

        fun initiate() {
            println("Initiate")
            val executor = Executors.newSingleThreadExecutor()

            executor.execute {
                while (client == null) {
                    try {
                        client = Socket(IPV4, PORT)
                    } catch (ignored: Exception) {
                        Logger.getLogger(MainActivity::class.java.name)
                            .warning("Retrying to connect in 5")
                        handler.post {
                            Toast.makeText(context, "Retrying", Toast.LENGTH_SHORT).show()
                        }
                        Thread.sleep(5000)
                        continue
                    }
                    Logger.getLogger(MainActivity::class.java.name).info("Connected")
                    handler.post {
                        Toast.makeText(context, "Server Connected", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }


        @Synchronized
        fun disconnect() {
            try {
                client?.close()
                handler.post {
                    Toast.makeText(context, "Server Disconnected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
            }
        }
    }

}