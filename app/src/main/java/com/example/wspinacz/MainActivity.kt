package com.example.wspinacz

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var appStatus = true

        val background = findViewById<LinearLayout>(R.id.main_screaen_background)  // background of the app status
        val turnOnOff = findViewById<Button>(R.id.fall_detection_status_change)  // button status of the app
        val appStatusText = findViewById<TextView>(R.id.fall_detection_status)  // text status of the app
        val apply = findViewById<Button>(R.id.settings_apply)  // button apply settings



        val fallDetectionService = FallDetectionService()  // creates an object detecting fall
        startFallDetectionService(fallDetectionService)  // Start the FallDetectionService


        // changes texts and colors of the app status segment
        turnOnOff.setOnClickListener{
            appStatus = !appStatus

            if (appStatus) {
                background.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                appStatusText.text = "working"
                turnOnOff.text = "Turn OFF"

                threshold = 40.0f
            }
            else {
                background.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                appStatusText.text = "not working"
                turnOnOff.text = "Turn ON"

                threshold = 100000.0f
            }
        }


        // applies settings
        apply?.setOnClickListener {
            timerTime = findViewById<EditText>(R.id.timer_time_set)?.text.toString().toLong()
            phone_number = findViewById<EditText>(R.id.phone_number_set).text.toString()
            text_message = findViewById<EditText>(R.id.sms_text_set).text.toString()
        }

    }





    // starts the fall detection
    private fun startFallDetectionService(fallDetectionService: FallDetectionService) {
        val serviceIntent = Intent(this, fallDetectionService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Starting from Android Oreo, use startForegroundService
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

    }







}
