package com.jerwintech.loginsample
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignInActivity : AppCompatActivity() {


    private var backPressedTime: Long = 0
    private lateinit var toast: Toast
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+(\\.+[a-z]+)?"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        checkUserLogin()

        val activityInfo = packageManager.getActivityInfo(componentName, 0)
        activityInfo.launchMode = ActivityInfo.LAUNCH_SINGLE_TOP

        firebaseAuth = FirebaseAuth.getInstance()

        val signInEmail : EditText = findViewById(R.id.emailEt)
        val signInPass : EditText = findViewById(R.id.passET)
        val signInButton : Button = findViewById(R.id.signInButton)
        val forgotPassText: TextView = findViewById(R.id.forgotPassText)



        val signUpText: TextView = findViewById(R.id.signInText)
        signUpText.setOnClickListener{
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        forgotPassText.setOnClickListener {
            val forgotPassEmail : EditText = findViewById(R.id.emailEt)
            val email = forgotPassEmail.text.toString()

            if (email.isEmpty()) {
                // Show an error message if the email is empty
                forgotPassEmail.error = "Email is required."
            } else {
                // Send a password reset email to the user's email address
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Show a success message if the password reset email was sent successfully
                            Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                        } else {
                            // Show an error message if there was a problem sending the password reset email
                            Toast.makeText(this, "Failed to send password reset email.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        signInButton.setOnClickListener{
            val email = signInEmail.text.toString()
            val pass = signInPass.text.toString()

            if (email.isEmpty() || pass.isEmpty()){
                if (email.isEmpty()){
                    signInEmail.error = "Enter Email Address"
                }
                if (pass.isEmpty()){
                    signInPass.error = "Enter Password"
                }
                Toast.makeText(this, "Enter Valid Details",Toast.LENGTH_SHORT).show()
            }else if(!email.matches(emailPattern.toRegex())){
                signInEmail.error = "Enter Valid Email Address"
                Toast.makeText(this, "Enter Valid Email Address",Toast.LENGTH_SHORT).show()
            }else if(pass.length < 6){
                signInPass.error = "Enter Password more than 6 characters"
                Toast.makeText(this, "Enter Password more than 6 characters",Toast.LENGTH_SHORT).show()
            }else{
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val uid = firebaseAuth.currentUser!!.uid
                        val database = FirebaseDatabase.getInstance("https://sweettrack-8f7e6-default-rtdb.firebaseio.com/").reference


                        database.child("users").child(uid).child("fname").addListenerForSingleValueEvent(object :
                            ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val userName = snapshot.value as? String
                                val sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putBoolean("isLoggedIn", true)
                                editor.putString("userName", userName)
                                editor.apply()


                                // Start the MainActivity
                                val intent = Intent(this@SignInActivity, MainActivity::class.java)
                                intent.putExtra("USER_NAME", userName)
                                startActivity(intent)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(TAG, "Failed to read value.", error.toException())
                            }
                        })
                    } else {
                        Toast.makeText(this, "Incorrect Login Credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }

    private fun checkUserLogin() {
        val sharedPreferences = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // User is already logged in, start the MainActivity
            val intent = Intent(this, MainActivity::class.java)
            val userName = sharedPreferences.getString("userName", null)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
            finish() // Optional: Finish the current activity so that the user can't navigate back to the login screen
        }
    }

    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            toast.cancel()
            val intent = Intent(this, SignInActivity::class.java)
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
}