package com.example.wspinacz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private val fallDetectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showFallAlert()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Register the BroadcastReceiver to listen for fall detection events
        val filter = IntentFilter("FALL_DETECTED")
        registerReceiver(fallDetectedReceiver, filter)

        // Start the FallDetectionService
        startFallDetectionService()
    }

    private fun startFallDetectionService() {
        val serviceIntent = Intent(this, FallDetectionService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Starting from Android Oreo, use startForegroundService
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun showFallAlert() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.fall_alert, null)

        //val alertMessage = dialogView.findViewById<TextView>(R.id.alertMessage)
        val countdownTimer = dialogView.findViewById<TextView>(R.id.countdownTimer)

        builder.setView(dialogView)
        builder.setCancelable(false)

        val alertDialog: AlertDialog = builder.create()

        // Implement your countdown logic here
        val countdown = object : CountDownTimer(15500, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownTimer.text = "Countdown: ${millisUntilFinished / 1000} seconds"
            }

            override fun onFinish() {
                // Countdown finished, handle accordingly
                alertDialog.dismiss()
                startFallDetectionService()
                // Add any necessary actions here
            }
        }

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes") { _, _ ->
            // Handle user response, e.g., call emergency services or perform other actions
            startFallDetectionService()
            countdown.cancel()
            alertDialog.dismiss()
        }

        alertDialog.show()
        countdown.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the BroadcastReceiver to avoid memory leaks
        unregisterReceiver(fallDetectedReceiver)
    }
}
