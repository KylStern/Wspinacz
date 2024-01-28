package com.example.wspinacz

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File

class MainActivity : AppCompatActivity() {





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var appStatus = true  // ON / OFF

        val background = findViewById<LinearLayout>(R.id.main_screaen_background)  // background of the app status
        val turnOnOff = findViewById<Button>(R.id.fall_detection_status_change)  // button status of the app
        val appStatusText = findViewById<TextView>(R.id.fall_detection_status)  // text status of the app
        val apply = findViewById<Button>(R.id.settings_apply)  // button apply settings

        loadSettings()

        val fallDetectionService = FallDetectionService()  // creates an object detecting fall
        startFallDetectionService(fallDetectionService)  // Start the FallDetectionService


        // changes texts and colors of the app status segment
        turnOnOff.setOnClickListener{
            appStatus = !appStatus

            if (appStatus) {
                startFallDetectionService(fallDetectionService)
                background.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                appStatusText.text = "working"
                turnOnOff.text = "Turn OFF"
                threshold = 40.0f
            }
            else {
                val serviceIntent = Intent(this, fallDetectionService::class.java)
                background.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                appStatusText.text = "not working"
                stopService(serviceIntent)
                turnOnOff.text = "Turn ON"
                threshold = 100000.0f
            }
        }


        // applies settings
        apply?.setOnClickListener {
            timerTime = findViewById<EditText>(R.id.timer_time_set)?.text.toString().toLong()
            phone_number = findViewById<EditText>(R.id.phone_number_set).text.toString()
            text_message = findViewById<EditText>(R.id.sms_text_set).text.toString()

            saveSettings()
            loadSettings()
        }

    }





    // starts the fall detection
    private fun startFallDetectionService(fallDetectionService: FallDetectionService) {
        val serviceIntent = Intent(this, fallDetectionService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }





    // saves users settings
    private fun saveSettings() {
        val settings = Settings(timerTime, phone_number, text_message)
        val gson = Gson()
        val jsonString :String = gson.toJson(settings)
        val file = File(cacheDir.absolutePath + "/settings.json" )
        file.writeText(jsonString)
    }





    // loads users settings
    private fun loadSettings() {
        // loads settings
        val gson = Gson()
        val bufferReader: BufferedReader = File(cacheDir.absolutePath + "/settings.json").bufferedReader()
        val inputString = bufferReader.use { it.readText() }
        val settings = gson.fromJson(inputString, Settings::class.java)

        // assigns the values
        timerTime = settings.timerTime.toString().toLong()
        phone_number = settings.phone_number.toString()
        text_message = settings.text_message.toString()

        // prints the values in the settings section
        findViewById<EditText>(R.id.timer_time_set).setText(timerTime.toString())
        findViewById<EditText>(R.id.phone_number_set).setText(phone_number)
        findViewById<EditText>(R.id.sms_text_set).setText(text_message)
    }





}
