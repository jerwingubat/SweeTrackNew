package com.example.sweetrack

import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.jerwintech.loginsample.databinding.ActivityDetailBinding
import com.jerwintech.loginsample.ml.ModelRegulerizer
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var bitmap: Bitmap
    private lateinit var toast: Toast
    private val database = FirebaseDatabase.getInstance("https://sweettrack-8f7e6-default-rtdb.firebaseio.com/")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val activityInfo = packageManager.getActivityInfo(componentName, 0)
        activityInfo.launchMode = ActivityInfo.LAUNCH_SINGLE_TOP
        //Prepare Tflite model and data
        val fileName = "labels.txt"
        val sugarName = "sugar.txt"
        val caloriesName = "calorie.txt"

        val inputString = application.assets.open(fileName).bufferedReader().use { it.readText() }
        val sugarString = application.assets.open(sugarName).bufferedReader().use { it.readText() }
        val caloriesString = application.assets.open(caloriesName).bufferedReader().use { it.readText() }

        val foodList = inputString.split("\n")
        val sugarList = sugarString.split("\n")
        val caloriesList = caloriesString.split("\n")

        //get uri image bitmap
        val imageUri: Uri? = intent.data
        bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

        //load image
        loadImage(imageUri)

        binding.btnPredicted.setOnClickListener {
            //implement Tflite
            val model = ModelRegulerizer.newInstance(this)

            val imageProcessor: ImageProcessor =
                ImageProcessor.Builder().add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                    .build()
            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // Creates inputs for reference.
            val inputFeature0 =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val byteBuffer = tensorImage.buffer
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val max = getMax(outputFeature0.floatArray)

            val resultFood = foodList[max]
            val resultSugar = sugarList[max]
            val resultCalorie = caloriesList[max]

            binding.foodName.text = "Your Food Is $resultFood"
            binding.foodDesc.text = "your sugar and calories take from $resultFood per serving"

            binding.btnSelectTake.setOnClickListener {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {

                    val uid = currentUser.uid

                    // Save the resultFood under the user's UID node in the database
                    val foodQuantityEditText : EditText = findViewById(R.id.foodquantity)
                    val foodQuantity = foodQuantityEditText.text.toString().toFloatOrNull() ?: 0f

                    val foodRef = database.getReference("users/$uid/foods/").push()
                    val foodData = HashMap<String, Any>()
                    foodData["food"] = resultFood
                    foodData["calorie"] = String.format("%.1f", resultCalorie.toFloat() * foodQuantity)
                    foodData["sugar"] = String.format("%.1f", resultSugar.toFloat() * foodQuantity)
                    foodData["timestamp"] = ServerValue.TIMESTAMP

                    // Save the data to the database
                    foodRef.setValue(foodData)

                    val dailyFoodsRef = database.getReference("users/$uid/dailyFoods/").push()
                    val dailyFoodData = HashMap<String, Any>()
                    dailyFoodData["food"] = resultFood
                    dailyFoodData["calorie"] = String.format("%.1f", resultCalorie.toFloat() * foodQuantity)
                    dailyFoodData["sugar"] = String.format("%.1f", resultSugar.toFloat() * foodQuantity)
                    dailyFoodData["timestamp"] = ServerValue.TIMESTAMP
                    dailyFoodsRef.setValue(dailyFoodData)
                    val weeklyFoodsRef = database.getReference("users/$uid/weeklyFoods/").push()
                    val weeklyFoodData = HashMap<String, Any>()
                    weeklyFoodData["food"] = resultFood
                    weeklyFoodData["calorie"] = String.format("%.1f", resultCalorie.toFloat() * foodQuantity)
                    weeklyFoodData["sugar"] = String.format("%.1f", resultSugar.toFloat() * foodQuantity)
                    weeklyFoodData["timestamp"] = ServerValue.TIMESTAMP
                    weeklyFoodsRef.setValue(weeklyFoodData)
                    val monthlyFoodsRef = database.getReference("users/$uid/monthlyFoods/").push()
                    val monthlyFoodData = HashMap<String, Any>()
                    monthlyFoodData["food"] = resultFood
                    monthlyFoodData["calorie"] = String.format("%.1f", resultCalorie.toFloat() * foodQuantity)
                    monthlyFoodData["sugar"] = String.format("%.1f", resultSugar.toFloat() * foodQuantity)
                    monthlyFoodData["timestamp"] = ServerValue.TIMESTAMP
                    monthlyFoodsRef.setValue(monthlyFoodData)


                    val databaseRef = FirebaseDatabase.getInstance("https://sweettrack-8f7e6-default-rtdb.firebaseio.com/").reference
                    databaseRef.child("users").child(uid).child("fname").addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val userName = snapshot.value as? String

                            // Start the MainActivity
                            val intent = Intent(this@DetailActivity, MainActivity::class.java)
                            intent.putExtra("USER_NAME", userName)
                            startActivity(intent)
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Log.e(ContentValues.TAG, "Failed to read value.", error.toException())
                        }
                    })
                    Toast.makeText(this, "Food saved!", Toast.LENGTH_LONG).show()
                }else {
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                    RC_SIGN_IN
                }
            }

            //calorie chart
            if (resultCalorie != null){
                val quantityLayout = findViewById<TextInputLayout>(R.id.quantityLayout)
                quantityLayout.visibility = View.GONE
                binding.calorieChart.visibility = View.VISIBLE
                binding.progbar.visibility = View.GONE
                calorieChart(resultCalorie)
            }else{
                binding.calorieChart.visibility = View.GONE
            }

            binding.btnPredicted.visibility = View.GONE
            binding.btnSelectTake.visibility = View.VISIBLE
            binding.btnBackToHome.visibility = View.VISIBLE
            binding.progbar.visibility = View.VISIBLE

            //sugar chart
            if (resultSugar != null){

                val quantityLayout = findViewById<TextInputLayout>(R.id.quantityLayout)
                quantityLayout.visibility = View.GONE
                binding.sugarChart.visibility = View.VISIBLE
                binding.progbar.visibility = View.GONE
                sugarChart(resultSugar)
            }else{
                binding.sugarChart.visibility = View.GONE
            }

        }

        binding.btnBackToHome.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {

                // User is already authenticated, start MainActivity
                val welcomeUser : TextView = findViewById(R.id.user)
                val userName = intent.getStringExtra("USER_NAME")
                welcomeUser.text = "Welcome, $userName!"
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }else {
                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
                RC_SIGN_IN
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // User has successfully authenticated, start MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                // Authentication failed, display an error message
                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Update UI with user's name
            val displayName = currentUser.displayName
            // ...
        }
    }

    companion object {
        const val RC_SIGN_IN = 123
    }


    //load image function
    private fun loadImage(image : Uri?){
        Log.d("photo", image.toString())
        if (image != null){
            binding.foodPhoto.setImageURI(image)
        }else{
            binding.foodName.text = "No Photo Input"
        }
    }
    //get value of data
    private fun getMax(arr:FloatArray) : Int{
        var ind = 0
        var min = 0.0f

        for (i in 0..100){
            if (arr[i] > min){
                ind = i
                min = arr[i]
            }
        }
        return ind
    }
    //chart function
    private fun calorieChart(data: String){
        val foodQuantityEditText = findViewById<EditText>(R.id.foodquantity)
        val foodQuantity = foodQuantityEditText.text.toString().toFloatOrNull() ?: 0f // get value from EditText, default to 1 if not a valid float

        val visitors: ArrayList<PieEntry> = ArrayList()
        val dataWithQuantity = data.toFloat() * foodQuantity // multiply data with food quantity
        visitors.add(PieEntry(dataWithQuantity, ""))
        val pieDataSet = PieDataSet(visitors, "Calories per Serving")
        pieDataSet.color = Color.rgb(157, 190, 185)
        pieDataSet.valueTextSize = 12f

        val pieData = PieData(pieDataSet)

        binding.apply {
            calorieChart.data = pieData
            calorieChart.description.isEnabled = false
            calorieChart.centerText = "Calories"
            calorieChart.animate()
        }
    }
    private fun sugarChart(resultSugar: String) {
        val foodQuantity = binding.foodquantity.text.toString().toFloat()
        val visitors: ArrayList<PieEntry> = ArrayList()

        if (resultSugar.toFloat() > 0.0){
            visitors.add(PieEntry(resultSugar.toFloat() * foodQuantity, ""))

            val pieDataSet = PieDataSet(visitors, "Sugar per Serving")
            pieDataSet.color = Color.rgb(255,192,203)
            pieDataSet.valueTextColor = Color.BLACK
            pieDataSet.valueTextSize = 12f

            val pieData = PieData(pieDataSet)

            binding.apply {
                sugarChart.data = pieData
                sugarChart.description.isEnabled = false
                sugarChart.centerText = "Sugar"
                sugarChart.animate()
            }
        }else{
            visitors.add(PieEntry(resultSugar.toFloat(), ""))

            val pieDataSet = PieDataSet(visitors, "Sugar / 100g")
            pieDataSet.color = Color.rgb(255,192,203)
            pieDataSet.valueTextColor = Color.BLACK
            pieDataSet.valueTextSize = 12f

            val pieData = PieData(pieDataSet)

            binding.apply {
                sugarChart.data = pieData
                sugarChart.description.isEnabled = false
                sugarChart.centerText = "No Sugar"
                sugarChart.animate()
            }
        }
    }

}