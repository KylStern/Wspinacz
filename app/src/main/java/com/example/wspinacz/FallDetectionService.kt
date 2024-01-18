package com.example.wspinacz

import android.app.*
import android.content.Context
import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

class FallDetectionService : Service() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var systemAlertView: View? = null
    private var countdownTimer: CountDownTimer? = null


    private val threshold = 40.0f // Adjust this threshold based on your testing

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            stopSelf()
        } else {
            startForegroundService()
            startAccelerometerListener()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorEventListener)
    }


    // Ustawienie Powiadomienia
    private fun startForegroundService() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel() else ""

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Fall Detection Service")
            .setContentText("Running in the background")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notificationBuilder)
    }


    // Wyświetla powiadomienie
    private fun createNotificationChannel(): String {
        val channelId = "fall_detection_channel"
        val channelName = "Fall Detection Service"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return channelId
    }


    // Włącza sensor
    private fun startAccelerometerListener() {
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }


    // Ustawienia sensora, aktualnie działa w każdym kierunku.
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt(x * x + y * y + z * z)

            if (acceleration > threshold) {
                //Jeżeli telefon "upada" wyłącza sensor, wyświetla alert
                showFallAlert()
                sensorManager.unregisterListener(this)
                stopSelf()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            //Aktualnie bezużyteczne, ale może się przydać
        }
    }


    //Wyświetla Alert
    private fun showFallAlert() {
        showSystemAlertWindow()
    }


    //Kod do wywołania Alert Boxa
    private fun showSystemAlertWindow() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        systemAlertView = inflater.inflate(R.layout.fall_alert, null)


        val countdownTextView = systemAlertView?.findViewById<TextView>(R.id.countdownTimer)
        val disableButton = systemAlertView?.findViewById<Button>(R.id.disableButton)


        // Po prostu timer
        countdownTimer = object : CountDownTimer(15500, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownTextView?.text = "Countdown: ${millisUntilFinished / 1000} seconds"
            }

            override fun onFinish() {
                removeSystemAlertWindow()
                sendSMS("TEST")
            }
        }
        countdownTimer?.start()

        disableButton?.setOnClickListener {
            onDisableButtonClick()
        }

        windowManager.addView(systemAlertView, layoutParams)
    }


    //Wyłącza alert
    private fun removeSystemAlertWindow() {
        if (systemAlertView != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(systemAlertView)
            systemAlertView = null
            countdownTimer?.cancel()
            countdownTimer = null
        }
    }
    //Przycisk wyłączania Alertu o upadku
    private fun onDisableButtonClick() {
        removeSystemAlertWindow()
        startForegroundService()
        startAccelerometerListener()
    }



    fun sendSMS(message: String) {
        val phoneNumber = "123456789"

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, proceed with sending SMS
            sendSmsWithPermissionGranted(phoneNumber, message)
        } else {
            println("Do zaimplementowania")
        }
    }

    private fun sendSmsWithPermissionGranted(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        val piSent = PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE)
        val piDelivered = PendingIntent.getBroadcast(this, 0, Intent("SMS_DELIVERED"),
            PendingIntent.FLAG_IMMUTABLE)

        // Split the message into parts if it's too long
        val parts = smsManager.divideMessage(message)
        val messageCount = parts.size

        for (i in 0 until messageCount) {
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                parts[i],
                piSent,
                piDelivered
            )
        }
    }

}

