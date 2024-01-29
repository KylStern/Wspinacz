package com.example.wspinacz

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {
    private var fusedLocationClient: FusedLocationProviderClient? = null
//    private var lastLocation: Location? = null
//    private var coordinates: String = ""






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        setContentView(R.layout.activity_main)

        var appStatus = true  // ON / OFF

        val background = findViewById<LinearLayout>(R.id.main_screaen_background)  // background of the app status
        val turnOnOff = findViewById<Button>(R.id.fall_detection_status_change)  // button status of the app
        val appStatusText = findViewById<TextView>(R.id.fall_detection_status)  // text status of the app
        val apply = findViewById<Button>(R.id.settings_apply)  // button apply settings

        loadSettings()
//        val x = getLastLocation()
//        println(x)


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
        val settings = UsersSettings(timerTime, phone_number, text_message)
        val gson = Gson()
        val jsonString :String = gson.toJson(settings)
        val file = File(cacheDir.absolutePath + "/settings.json" )
        file.writeText(jsonString)
    }





    // loads users settings
    private fun loadSettings() {
        // loads settings
        val gson = Gson()
        val file = File(cacheDir.absolutePath + "/settings.json")
        if (file.exists()){
            val bufferReader: BufferedReader = file.bufferedReader()
            val inputString = bufferReader.use { it.readText() }
            val settings = gson.fromJson(inputString, UsersSettings::class.java)

            // assigns the values
            timerTime = settings.timerTime.toString().toLong()
            phone_number = settings.phone_number.toString()
            text_message = settings.text_message.toString()
        }
        else {
            // assigns the values
            timerTime = "30".toLong()
            phone_number = "888906447"
            text_message = "Help me"
        }

        // prints the values in the settings section
        findViewById<EditText>(R.id.timer_time_set).setText(timerTime.toString())
        findViewById<EditText>(R.id.phone_number_set).setText(phone_number)
        findViewById<EditText>(R.id.sms_text_set).setText(text_message)

    }










//    @SuppressLint("MissingPermission")
//    private fun getLastLocation(): String? {
//        var coordinates: String? = ""
//        fusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
//            if (task.isSuccessful && task.result != null) {
//                val lastLocation = task.result
//                coordinates = ", my coordinates: ${(lastLocation)!!.latitude}, ${(lastLocation)!!.longitude}"
//                println(coordinates)
//
//            }
//            else {
//                Log.w(TAG, "getLastLocation:exception", task.exception)
//            }
//        }
//        return coordinates





//        var coordinates: String? = ""
//        val task = fusedLocationClient?.lastLocation
//        if (task != null) {
//            task.addOnCompleteListener { task ->
//                if (task.isSuccessful && task.result != null) {
//                    val lastLocation = task.result
//                    coordinates = ", my coordinates: ${(lastLocation)!!.latitude}, ${(lastLocation)!!.longitude}"
//                }
//                else {
//                    Log.w(TAG, "getLastLocation:exception", task.exception)
//                }
//            }
//            // Wait until the task is completed
////            task.await()
//        }
//        return coordinates
//    }







}