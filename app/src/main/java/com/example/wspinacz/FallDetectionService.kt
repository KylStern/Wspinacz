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
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.telephony.SmsManager
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import kotlin.math.sqrt
import kotlin.properties.Delegates

var threshold = 40.0f  // threshold of the accelerometer
var timerTime: Long = 30  // countdown time
var text_message: String = ""  // text message send for given number
var phone_number: String = ""  // number to which the phone will send the given message
class FallDetectionService : Service() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var systemAlertView: View? = null
    private var countdownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null


    companion object {
        const val REQUEST_CODE = 123
    }










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
//
//        if (!requestSystemAlertWindowPermission()) {
//            showSystemAlertWindow()
//        }
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }



    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
        sensorManager.unregisterListener(sensorEventListener)
    }










    // notification settings
    private fun startForegroundService() {
        val channelId =
            createNotificationChannel()
        println("dziala")
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Fall Detection Service")
            .setContentText("Running in the background")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notificationBuilder)
    }

    // notification print
    private fun createNotificationChannel(): String {
        val channelId = "fall_detection_channel"
        val channelName = "Fall Detection Service"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return channelId
    }










    // turn the sensor ON
    private fun startAccelerometerListener() {
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }



    // sensor settings
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt(x * x + y * y + z * z)

            if (acceleration > threshold) {
                // if phone falls: shows the alert, turns the sensor OFF
                showFallAlert()
                sensorManager.unregisterListener(this)
                stopSelf()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }










    // show alert
    private fun showFallAlert() {
        showSystemAlertWindow()
    }



    // show alert
    private fun showSystemAlertWindow() {
        playAlarmSound()
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





        // Timer
        countdownTimer = object : CountDownTimer(timerTime * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownTextView?.text = "Countdown: ${millisUntilFinished / 1000} seconds"
            }

            override fun onFinish() {
                removeSystemAlertWindow()
                sendSMS(text_message)
            }
        }
        countdownTimer?.start()

        disableButton?.setOnClickListener {
            onDisableButtonClick()
            stopAlarmSound()
            stopSelf()
            onCreate()
        }

        windowManager.addView(systemAlertView, layoutParams)
    }










    // turns the alert OFF
    private fun removeSystemAlertWindow() {
        if (systemAlertView != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(systemAlertView)
            systemAlertView = null
            countdownTimer?.cancel()
            countdownTimer = null
        }
    }



    // turning the alert OFF
    private fun onDisableButtonClick() {
        removeSystemAlertWindow()
        startAccelerometerListener()
        stopAlarmSound()
        startForegroundService()

        val restartIntent = Intent(this, FallDetectionService::class.java)
        startService(restartIntent)
    }









    // sends given text message to given phone number
    fun sendSMS(message: String) {
        val phoneNumber = phone_number

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



    // sends given text message to given phone number
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










    // turns the sound alarm ON
    private fun playAlarmSound() {
        try {
            // Use the media player to play the alarm sound
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer?.setAudioAttributes(audioAttributes)

            // Start the media player
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    // turns the sound alarm OFF
    private fun stopAlarmSound() {
        mediaPlayer?.release()
        mediaPlayer = null
    }










    // grand permission to open alert box
    private fun requestSystemAlertWindowPermission(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            return true
        }
        return false
    }





}

