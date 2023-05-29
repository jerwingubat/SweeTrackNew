package com.jerwintech.loginsample

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private var backPressedTime: Long = 0
    private lateinit var toast: Toast
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+(\\.+[a-z]+)?"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)


        val activityInfo = packageManager.getActivityInfo(componentName, 0)
        activityInfo.launchMode = ActivityInfo.LAUNCH_SINGLE_TOP

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://sweettrack-8f7e6-default-rtdb.firebaseio.com/")

        val signUpFname : EditText = findViewById(R.id.fnameEt)
        val signUpLname : EditText = findViewById(R.id.lnameEt)
        val signUpEmail : EditText = findViewById(R.id.emailEt)
        val signUpPass : EditText = findViewById(R.id.passET)
        val signUpConfirmPass : EditText = findViewById(R.id.confirmPassEt)
        val signUpButton : Button = findViewById(R.id.signUpButton)

        val signInText: TextView = findViewById(R.id.signUpText)
        signInText.setOnClickListener{
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        val diabetesTypeSpinner: Spinner = findViewById(R.id.diabetesSpinner)
        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            this,
            R.array.diabetes_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        diabetesTypeSpinner.adapter = adapter
        diabetesTypeSpinner.prompt = getString(R.string.select_diabetes_type_hint)


        signUpButton.setOnClickListener{
            val fname = signUpFname.text.toString()
            val lname = signUpLname.text.toString()
            val email = signUpEmail.text.toString()
            val pass = signUpPass.text.toString()
            val confirmPass = signUpConfirmPass.text.toString()

            if (fname.isEmpty() || lname.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()){
                if (fname.isEmpty()){
                    signUpFname.error = "Enter First Name"
                }
                if (lname.isEmpty()){
                    signUpLname.error = "Enter Last Name"
                }
                if (email.isEmpty()){
                    signUpEmail.error = "Enter Email"
                }
                if (pass.isEmpty()){
                    signUpFname.error = "Enter Password"
                }
                if (confirmPass.isEmpty()){
                    signUpConfirmPass.error = "Re-enter Password"
                }
                Toast.makeText(this, "Enter Valid Details",Toast.LENGTH_SHORT).show()
            }else if(!email.matches(emailPattern.toRegex())){
                signUpEmail.error = "Enter Valid Email Address"
                Toast.makeText(this, "Enter Valid Email Address",Toast.LENGTH_SHORT).show()
            }else if(pass.length < 6){
                signUpPass.error = "Enter Password more than 6 characters"
                Toast.makeText(this, "Enter Password more than 6 characters",Toast.LENGTH_SHORT).show()
            }else if(pass != confirmPass){
                signUpConfirmPass.error = "Password not matched, try again"
                Toast.makeText(this, "Password not matched, try again",Toast.LENGTH_SHORT).show()
            }else{
                firebaseAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener{
                    if(it.isSuccessful){

                        val diabetesType = diabetesTypeSpinner.selectedItem.toString()
                        val databaseRef = database.reference.child("users").child(firebaseAuth.currentUser!!.uid)
                        val users : Users = Users(fname, lname, email,diabetesType, firebaseAuth.currentUser!!.uid)

                        databaseRef.setValue(users).addOnCompleteListener {
                            if(it.isSuccessful){
                                val intent = Intent(this,SignInActivity::class.java)
                                startActivity(intent)
                            }else{
                                Toast.makeText(this, "Incorrect",Toast.LENGTH_SHORT).show()
                            }
                        }

                    }else{
                        Toast.makeText(this, "Something went wrong: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    override fun onBackPressed() {
        // do nothing
    }
}