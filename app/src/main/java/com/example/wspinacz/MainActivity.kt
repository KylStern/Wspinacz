package com.example.wspinacz

import android.content.BroadcastReceiver
import android.content.Context
import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private val fallDetectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showFallAlert()
        }
    }

    private val SEND_SMS_PERMISSION_REQUEST_CODE = 123
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestSmsPermission()
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




    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
        } else {
            // Request the SEND_SMS permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SEND_SMS_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SEND_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with sending SMS
            } else {
                // Permission denied
                // Handle the case where the user denied the SMS permission
                // You might want to show a message to the user or take appropriate action
            }
        }
    }
}
