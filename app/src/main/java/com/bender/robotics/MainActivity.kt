package com.bender.robotics

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    // CHANGE PORT AND IPV4 HERE
    companion object {
        const val PORT = 12523
        var IPV4 = "loc"
    }

    private lateinit var mService: SocketService
    private var mBound: Boolean = false

    private val prefName = "IP_ADDRESS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onClickListeners()
    }

    private val connection = object: ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SocketService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }

    }

    override fun onStart() {
        super.onStart()
        Intent(this, SocketService::class.java).also{
            intent -> bindService(intent,connection,Context.BIND_AUTO_CREATE)
        }
    }


    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    /**
     * Button click listeners
     */
    private fun onClickListeners() {
        findViewById<Button>(R.id.forward).setOnClickListener {
            mService.write("1")
        }
        findViewById<Button>(R.id.backward).setOnClickListener {
            mService.write("2")
        }
        findViewById<Button>(R.id.right).setOnClickListener {
            mService.write("3")
        }
        findViewById<Button>(R.id.left).setOnClickListener {
            mService.write("4")
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
                    mService.initiate()
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
                mService.disconnect(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        mService.disconnect(false)
        super.onDestroy()
    }

}