package com.jerwintech.loginsample

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import com.jerwintech.loginsample.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class TimelineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timeline)


        val listView: ListView = findViewById(R.id.timeline_listview)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val database = FirebaseDatabase.getInstance().reference
            val childReference = database.child("users").child(uid).child("foods")
            childReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Get data as a List of Maps
                    val dataList = mutableListOf<Map<String, String>>()
                    for (dataSnapshotChild in dataSnapshot.children) {
                        val userData = dataSnapshotChild.value as Map<String, String>
                        val name = userData["food"]
                        val calorie = userData["calorie"].toString()
                        val sugar = userData["sugar"].toString()
                        val timestampString = userData["timestamp"].toString()
                        if (name != null && calorie != null && sugar != null) {

                            val timestamp = timestampString.toLong() // Convert seconds to milliseconds
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault())
                            dateFormat.timeZone = TimeZone.getDefault()
                            val date = dateFormat.format(Date(timestamp))
                            val map = mapOf("food" to name, "calorie" to calorie, "sugar" to sugar, "timestamp" to date)
                            dataList.add(map)
                        }
                    }

                    // Populate ListView with data
                    val adapter = SimpleAdapter(
                        this@TimelineActivity, dataList,
                        R.layout.list_item_food,
                        arrayOf("food", "calorie", "sugar","timestamp"),
                        intArrayOf(R.id.food_name, R.id.calorie, R.id.sugar,R.id.timestamp)
                    )
                    listView.adapter = adapter
                    adapter.notifyDataSetChanged()
                    listView.requestLayout()

                    // Initialize variables to accumulate total calories and sugar
                    var totalCalorie = 0.0
                    var totalSugar = 0.0

                    // Iterate through dataList to calculate total calories and sugar
                    for (data in dataList) {
                        totalCalorie += data["calorie"]?.toDouble()!!
                        totalSugar += data["sugar"]?.toDouble()!!
                    }

                    // Set up the data for the calorie pie chart
                    val caloriePieEntries = mutableListOf<PieEntry>()
                    caloriePieEntries.add(PieEntry(totalCalorie.toFloat(), ""))
                    caloriePieEntries.add(PieEntry(0f, "")) // Add an empty entry to make the chart look better

                    val caloriePieDataSet = PieDataSet(caloriePieEntries, "Total Calories Consumed")
                    caloriePieDataSet.colors = listOf(Color.rgb(157, 190, 185))

                    val caloriePieData = PieData(caloriePieDataSet)

                    // Set up the calorie pie chart
                    val caloriePieChart = findViewById<PieChart>(R.id.timeline_calorie_chart)
                    caloriePieChart.setUsePercentValues(false)
                    caloriePieChart.description.isEnabled = false
                    caloriePieChart.data = caloriePieData
                    caloriePieChart.centerText = "Calories"
                    caloriePieChart.animate()
                    caloriePieChart.invalidate()

                    // Set up the data for the sugar pie chart
                    val sugarPieEntries = mutableListOf<PieEntry>()
                    sugarPieEntries.add(PieEntry(totalSugar.toFloat(), ""))
                    sugarPieEntries.add(PieEntry(0f, "")) // Add an empty entry to make the chart look better

                    val sugarPieDataSet = PieDataSet(sugarPieEntries, "Total Sugar Consumed")
                    sugarPieDataSet.colors = listOf(Color.rgb(255,192,203))

                    val sugarPieData = PieData(sugarPieDataSet)

                    // Set up the sugar pie chart
                    val sugarPieChart = findViewById<PieChart>(R.id.timeline_sugar_chart)
                    sugarPieChart.description.isEnabled = false
                    sugarPieChart.animate()
                    sugarPieChart.centerText = "Sugar"
                    sugarPieChart.setUsePercentValues(false)
                    sugarPieChart.data = sugarPieData
                    sugarPieChart.invalidate()

                }


                override fun onCancelled(error: DatabaseError) {
                    Log.e(ContentValues.TAG, "Failed to read value.", error.toException())
                }
            })
        }else {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            DetailActivity.RC_SIGN_IN
        }
    }

}