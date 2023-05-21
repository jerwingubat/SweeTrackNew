package com.example.sweetrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jerwintech.loginsample.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var backPressedTime: Long = 0
    private lateinit var toast: Toast
    private lateinit var binding : ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var bitmap : Bitmap
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var logoutButton: Button
    private lateinit var databaseRef: FirebaseDatabase
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val database: DatabaseReference = FirebaseDatabase.getInstance("https://sweettrack-8f7e6-default-rtdb.firebaseio.com/").reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        scheduleReset()
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance("https://sweettrack-8f7e6-default-rtdb.firebaseio.com/")
        deleteDailyFoodsIfDateMismatch()


        logoutButton = findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            confirmLogout()
        }

        val timelineButton: Button = findViewById(R.id.timeline_button)
        timelineButton.setOnClickListener{

            val intent = Intent(this, TimelineActivity::class.java)
            startActivity(intent)
        }
        val logButton: Button = findViewById(R.id.log_button)
        logButton.setOnClickListener{

            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
        }

        val activityInfo = packageManager.getActivityInfo(componentName, 0)
        activityInfo.launchMode = ActivityInfo.LAUNCH_SINGLE_TOP

        val welcomeUser : TextView = findViewById(R.id.user)
        val userName = intent.getStringExtra("USER_NAME")
        welcomeUser.text = "Welcome, $userName!"

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null){
            val intent = Intent(this,SignInActivity::class.java)
            startActivity(intent)
        }
        when(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            PackageManager.PERMISSION_GRANTED -> {

            }
            else -> {
                checkForPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, "write storage", 102)
            }
        }
        binding.btnSelect.setOnClickListener {
            when(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                PackageManager.PERMISSION_GRANTED -> {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "image/*"

                    startActivityForResult(intent, 100)
                }
                else -> {
                    checkForPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, "storage", 100)
                }
            }
        }
        binding.btnPicture.setOnClickListener {
            when(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)){
                PackageManager.PERMISSION_GRANTED -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, 101)
                }
                else -> {
                    checkForPermissions(Manifest.permission.CAMERA, "camera", 101)
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK){
            val uri : Uri? = data?.data
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            val intent = Intent(this, DetailActivity::class.java)
            intent.data = uri
            startActivity(intent)
        }

        if (requestCode == 101 && resultCode == RESULT_OK){
            val bytes = ByteArrayOutputStream()
            bitmap = data?.getParcelableExtra("data")!!
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(this.contentResolver, bitmap, null, null)
            val imgPhoto = Uri.parse(path)
            val intent = Intent(this, DetailActivity::class.java)
            intent.data = imgPhoto
            startActivity(intent)
        }

    }
    private fun checkForPermissions(permission: String, name: String, requestCode: Int){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            when{
                ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED -> {
                }
                shouldShowRequestPermissionRationale(permission) -> showDialog(permission, name, requestCode)
                else -> ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fun innerCheck(name: String){
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext, "$name Permission Rejected", Toast.LENGTH_SHORT).show()
            }
        }
        when(requestCode){
            100 -> innerCheck("storage")
            101 -> innerCheck("camera")
            102 -> innerCheck("write storage")
        }
    }
    private fun showDialog(permission: String, name: String, requestCode: Int){
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage("Permission required $name")
            setTitle("Permission required")
            setPositiveButton("Ok"){ _, _ ->
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            }
        }
        val dialog = builder.create()
        dialog.show()
    }
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            toast.cancel()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            super.onBackPressed()
            finishAffinity()
        } else {
            toast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT)
            toast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }
    private fun logout() {
        val sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", false)
        editor.remove("userName")
        editor.apply()
        val intent = Intent(this, SignInActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("slideAnim", "left") // Add extra data to indicate the animation direction
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left) // Set the transition animation
        finish()
    }
    private fun confirmLogout() {
        // Create confirmation dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to log out?")
        builder.setPositiveButton("Yes") { _, _ ->
            logout()
        }
        builder.setNegativeButton("No", null)

        // Show confirmation dialog
        val dialog = builder.create()
        dialog.show()
    }
    private fun resetDailyFoods() {
        // Reset the dailyFoods node
        val dailyFoodsRef = database.child("users").child(userId!!).child("dailyFoods")
        dailyFoodsRef.removeValue()
    }
    private fun scheduleReset() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1) // Add 1 day to the current date/time

        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                resetDailyFoods()
                // Reschedule the reset operation for the next day
                scheduleReset()
            }
        }

        // Schedule the timer task to execute at the specified time
        timer.schedule(timerTask, calendar.time)
    }
    private fun deleteDailyFoodsIfDateMismatch() {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            val reference = databaseRef.getReference("users").child(userId).child("dailyFoods")

            reference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val currentDate = Calendar.getInstance().time

                    for (childSnapshot in dataSnapshot.children) {
                        val timestampValue = childSnapshot.child("timestamp").value
                        if (timestampValue is Long) {
                            val timestamp = convertLongToDate(timestampValue)

                            if (!isSameDate(currentDate, timestamp)) {
                                childSnapshot.ref.removeValue()
                                    .addOnSuccessListener {
                                        // Node values deleted successfully
                                        Log.d(TAG, "Node values deleted successfully")
                                    }
                                    .addOnFailureListener { error ->
                                        // Handle error while deleting node values
                                        Log.e(TAG, "Error deleting node values: $error")
                                    }
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle onCancelled event
                }
            })
        }
    }
    private fun convertLongToDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    private fun isSameDate(date1: Date, date2: Date): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString1 = format.format(date1)
        val dateString2 = format.format(date2)
        return dateString1 == dateString2
    }

    companion object {
        private const val TAG = "MainActivity"
    }


}