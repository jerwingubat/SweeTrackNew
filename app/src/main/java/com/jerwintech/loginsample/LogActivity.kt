package com.jerwintech.loginsample

import android.content.ContentValues.TAG
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jerwintech.loginsample.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class LogActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var sugarPieChart: PieChart
    private lateinit var caloriePieChart: PieChart
    private lateinit var weeklySugarPieChart: PieChart
    private lateinit var weeklyCaloriePieChart: PieChart
    private lateinit var monthlySugarPieChart: PieChart
    private lateinit var monthlyCaloriePieChart: PieChart
    private lateinit var database: DatabaseReference
    private lateinit var databaseRef: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private var totalCalorie = 0.0
    private var totalSugar = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        handler.postDelayed(resetValuesRunnable, getTimeUntilMidnight())

        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance("https://sweettrack-8f7e6-default-rtdb.firebaseio.com/")
        deleteDailyFoodsIfDateMismatch()


        database = FirebaseDatabase.getInstance().reference
        sugarPieChart = findViewById(R.id.log_sugar_chart)
        caloriePieChart = findViewById(R.id.log_calorie_chart)
        weeklySugarPieChart = findViewById(R.id.weekly_sugar_chart)
        weeklyCaloriePieChart = findViewById(R.id.weekly_calorie_chart)
        monthlySugarPieChart = findViewById(R.id.monthly_sugar_chart)
        monthlyCaloriePieChart = findViewById(R.id.monthly_calorie_chart)


        sugarPieChart.setUsePercentValues(false)
        sugarPieChart.description.isEnabled = false
        sugarPieChart.legend.isEnabled = true
        caloriePieChart.setUsePercentValues(false)
        caloriePieChart.description.isEnabled = false
        caloriePieChart.legend.isEnabled = true

        weeklySugarPieChart.setUsePercentValues(false)
        weeklySugarPieChart.description.isEnabled = false
        weeklySugarPieChart.legend.isEnabled = true
        weeklyCaloriePieChart.setUsePercentValues(false)
        weeklyCaloriePieChart.description.isEnabled = false
        weeklyCaloriePieChart.legend.isEnabled = true

        monthlySugarPieChart.setUsePercentValues(false)
        monthlySugarPieChart.description.isEnabled = false
        monthlySugarPieChart.legend.isEnabled = true
        monthlyCaloriePieChart.setUsePercentValues(false)
        monthlyCaloriePieChart.description.isEnabled = false
        monthlyCaloriePieChart.legend.isEnabled = true


        val dateToday: TextView = findViewById(R.id.date_today)
        val currentDate = Date()
        val inputDateFormat = SimpleDateFormat("dd-MM-yyyy")
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy")
        val inputDateStr = inputDateFormat.format(currentDate)
        val outputDateStr = outputDateFormat.format(inputDateFormat.parse(inputDateStr))
        dateToday.text = outputDateStr


        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val sugarReminder: TextView = findViewById(R.id.sugar_reminder)
            val calorieReminder: TextView = findViewById(R.id.calorie_reminder)
            val uid = currentUser.uid
            val database = FirebaseDatabase.getInstance().reference
            val childReference = database.child("users").child(uid).child("foods")
            childReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var totalCalories = 0.0
                    var totalSugar = 0.0
                    for (dataSnapshotChild in dataSnapshot.children) {
                        val userData = dataSnapshotChild.value as Map<String, String>
                        val calorie = userData["calorie"]?.toFloatOrNull()?.let { String.format("%.1f", it).toFloat() }
                        val sugar = userData["sugar"]?.toFloatOrNull()?.let { String.format("%.2f", it).toFloat() }
                        if (calorie != null && sugar != null) {
                            totalCalories += calorie
                            totalSugar += sugar
                        }
                    }
                    val formattedTotalSugar = String.format("%.2f", totalSugar)
                    calorieReminder.text = "Total Calories: $totalCalories Kcal"
                    sugarReminder.text = "Total Sugar: $formattedTotalSugar grams"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "Failed to retrieve foods data: ${databaseError.message}")
                }
            })
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            database.child("users").child(userId).child("diabetesType").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val diabetesType = dataSnapshot.getValue(String::class.java)
                    Log.d(TAG, "User diabetes type: $diabetesType")

                    // Add null check for diabetesType
                    if (diabetesType != null) {
                        // Retrieve daily sugar and calorie intake limits for the user's diabetes type from Firebase
                        database.child("users").child("dailyIntake").child(diabetesType).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val diabetesTypeLimits = dataSnapshot.getValue(DiabetesTypesLimits::class.java)
                                val sugarLimit = dataSnapshot.child("sugarLimit").getValue(Double::class.java)
                                val calorieLimit = dataSnapshot.child("calorieLimit").getValue(Double::class.java)

                                if (diabetesTypeLimits != null) {
                                    // Display the daily sugar and calorie intake limits in the pie charts
                                    displaySugarLimitInPieChart(diabetesTypeLimits)
                                    displayCalorieLimitInPieChart(diabetesTypeLimits)
                                    Log.d(TAG, "Sugar limit and Calorie Limit: $diabetesTypeLimits")

                                    // Query the Firebase database to get the sum of calorie and sugar intake
                                    database.child("users").child(userId).child("dailyFoods").addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            var totalCalorie = 0.0
                                            var totalSugar = 0.0
                                            for (dataSnapshotChild in dataSnapshot.children) {
                                                val userData = dataSnapshotChild.value as Map<String, String>
                                                val calorie = userData["calorie"]?.toDoubleOrNull()
                                                val sugar = userData["sugar"]?.toDoubleOrNull()
                                                if (calorie != null && sugar != null) {
                                                    totalCalorie += calorie
                                                    totalSugar += sugar
                                                }
                                            }
                                            displaySugarIntakeInPieChart(totalSugar, diabetesTypeLimits.sugarLimit)
                                            displayCalorieIntakeInPieChart(totalCalorie, diabetesTypeLimits.calorieLimit)

                                            // Display the total calorie and sugar intake along with the limits
                                            val sugarReminder: TextView = findViewById(R.id.sugar_reminder)
                                            val calorieReminder: TextView = findViewById(R.id.calorie_reminder)
                                            val calorieText = "Calorie Intake: $totalCalorie Kcal / $calorieLimit Kcal"
                                            val sugarText = "Sugar Intake: $totalSugar grams / $sugarLimit grams"
                                            sugarReminder.text = calorieText
                                            calorieReminder.text = sugarText
                                            Log.d(TAG, "Total Calorie: $totalCalorie, Total Sugar: $totalSugar")
                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {
                                            Log.w(TAG, "Failed to retrieve food data: ${databaseError.message}")
                                        }
                                    })
                                } else {
                                    Log.d(TAG, "No data found for diabetes type $diabetesType")
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.w(TAG, "Failed to retrieve diabetes type limits: ${databaseError.message}")
                            }
                        })

                        database.child("users").child("weeklyIntake").child(diabetesType).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val diabetesWeeklyLimits = dataSnapshot.getValue(DiabetesWeeklyLimit::class.java)
                                val sugarLimit = dataSnapshot.child("sugarLimit").getValue(Double::class.java)
                                val calorieLimit = dataSnapshot.child("calorieLimit").getValue(Double::class.java)

                                if (diabetesWeeklyLimits != null) {
                                    // Display the daily sugar and calorie intake limits in the pie charts
                                    displayWeeklySugarLimitInPieChart(diabetesWeeklyLimits)
                                    displayWeeklyCalorieLimitInPieChart(diabetesWeeklyLimits)
                                    Log.d(TAG, "Sugar limit and Calorie Limit: $diabetesWeeklyLimits")

                                    // Query the Firebase database to get the sum of calorie and sugar intake
                                    database.child("users").child(userId).child("weeklyFoods").addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            var totalCalorie = 0.0
                                            var totalSugar = 0.0
                                            for (dataSnapshotChild in dataSnapshot.children) {
                                                val userData = dataSnapshotChild.value as Map<String, String>
                                                val calorie = userData["calorie"]?.toDoubleOrNull()
                                                val sugar = userData["sugar"]?.toDoubleOrNull()
                                                if (calorie != null && sugar != null) {
                                                    totalCalorie += calorie
                                                    totalSugar += sugar
                                                }
                                            }
                                            displayWeeklySugarIntakeInPieChart(totalSugar, diabetesWeeklyLimits.sugarLimit)
                                            displayWeeklyCalorieIntakeInPieChart(totalCalorie, diabetesWeeklyLimits.calorieLimit)

                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {
                                            Log.w(TAG, "Failed to retrieve food data: ${databaseError.message}")
                                        }
                                    })
                                } else {
                                    Log.d(TAG, "No data found for diabetes type $diabetesType")
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.w(TAG, "Failed to retrieve diabetes type limits: ${databaseError.message}")
                            }
                        })

                        database.child("users").child("monthlyIntake").child(diabetesType).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val diabetesMonthlyLimits = dataSnapshot.getValue(DiabetesMonthlyLimit::class.java)
                                val sugarLimit = dataSnapshot.child("sugarLimit").getValue(Double::class.java)
                                val calorieLimit = dataSnapshot.child("calorieLimit").getValue(Double::class.java)

                                if (diabetesMonthlyLimits != null) {
                                    // Display the daily sugar and calorie intake limits in the pie charts
                                    displayMonthlySugarLimitInPieChart(diabetesMonthlyLimits)
                                    displayMonthlyCalorieLimitInPieChart(diabetesMonthlyLimits)
                                    Log.d(TAG, "Sugar limit and Calorie Limit: $diabetesMonthlyLimits")

                                    // Query the Firebase database to get the sum of calorie and sugar intake
                                    database.child("users").child(userId).child("monthlyFoods").addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            var totalCalorie = 0.0
                                            var totalSugar = 0.0
                                            for (dataSnapshotChild in dataSnapshot.children) {
                                                val userData = dataSnapshotChild.value as Map<String, String>
                                                val calorie = userData["calorie"]?.toDoubleOrNull()
                                                val sugar = userData["sugar"]?.toDoubleOrNull()
                                                if (calorie != null && sugar != null) {
                                                    totalCalorie += calorie
                                                    totalSugar += sugar
                                                }
                                            }
                                            displayMonthlySugarIntakeInPieChart(totalSugar, diabetesMonthlyLimits.sugarLimit)
                                            displayMonthlyCalorieIntakeInPieChart(totalCalorie, diabetesMonthlyLimits.calorieLimit)

                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {
                                            Log.w(TAG, "Failed to retrieve food data: ${databaseError.message}")
                                        }
                                    })
                                } else {
                                    Log.d(TAG, "No data found for diabetes type $diabetesType")
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.w(TAG, "Failed to retrieve diabetes type limits: ${databaseError.message}")
                            }
                        })


                    } else {
                        Log.d(TAG, "No diabetes type found for user")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "Failed to retrieve diabetes type: ${databaseError.message}")
                }
            })
        }



    }

    private fun resetValues() {
        totalCalorie = 0.0
        totalSugar = 0.0
    }

    private fun getTimeUntilMidnight(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis - System.currentTimeMillis()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val resetValuesRunnable = object : Runnable {
        override fun run() {
            resetValues()
            handler.postDelayed(this, 24 * 60 * 60 * 1000) // Schedule next reset in 24 hours
        }
    }

    private fun displaySugarLimitInPieChart(diabetesTypeLimits: DiabetesTypesLimits) {
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(diabetesTypeLimits.sugarLimit.toFloat(), ""))

        val dataSet = PieDataSet(entries, "Daily Sugar Intake Limit")
        dataSet.colors = listOf(Color.rgb(255,192,203))
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        data.setValueTextSize(16f)
        data.setValueTextColor(Color.BLACK)

        sugarPieChart.data = data
        sugarPieChart.invalidate()
    }
    private fun displayCalorieLimitInPieChart(diabetesTypeLimits: DiabetesTypesLimits) {
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(diabetesTypeLimits.calorieLimit.toFloat(), ""))

        val dataSet = PieDataSet(entries, "Daily Calorie Intake Limit")
        dataSet.colors = listOf(Color.rgb(157, 190, 185))
        dataSet.valueTextColor = Color.BLACK


        val data = PieData(dataSet)
        data.setValueTextSize(16f)
        data.setValueTextColor(Color.BLACK)

        caloriePieChart.data = data
        caloriePieChart.invalidate()
    }
    private fun displaySugarIntakeInPieChart(totalSugar: Double, sugarLimit: Double) {
        // Calculate the remaining sugar limit
        val remainingSugarLimit = sugarLimit - totalSugar

        // Create the pie chart entries for total sugar intake and remaining sugar limit
        val entries = listOf(
            PieEntry(totalSugar.toFloat(), "Total Sugar Intake"),
            PieEntry(remainingSugarLimit.toFloat(), "Remaining Sugar Limit")
        )

        // Create the pie chart data set and customize its appearance
        val dataSet = PieDataSet(entries, "")
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        // Set the colors for the pie chart slices
        val colors = mutableListOf<Int>()
        if (remainingSugarLimit <= 0) {
            // Remaining sugar limit is 0 or negative, set color to red
            colors.add(ContextCompat.getColor(this, R.color.alert_color))
            colors.add(ContextCompat.getColor(this,R.color.soft_red))
        } else {
            // Remaining sugar limit is positive, set regular colors
            colors.add(ContextCompat.getColor(this, R.color.hard_pink))
            colors.add(ContextCompat.getColor(this, R.color.soft_pink))
        }
        dataSet.colors = colors

        // Create the pie chart and customize its appearance
        val pieChart: PieChart = findViewById(R.id.log_sugar_chart)
        pieChart.setUsePercentValues(false)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.dark_blue))

        // Set the data for the pie chart and refresh its display
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()

        if (remainingSugarLimit <= 0) {
            val textView: TextView = findViewById(R.id.sugar_intake_warning)
            textView.text = "DAILY SUGAR INTAKE EXCEEDED"
        }
    }

    private fun displayCalorieIntakeInPieChart(totalCalorie: Double, calorieLimit: Double) {
        // Calculate the remaining calorie limit
        val remainingCalorieLimit = calorieLimit - totalCalorie

        // Create the pie chart entries for total calorie intake and remaining calorie limit
        val entries = listOf(
            PieEntry(totalCalorie.toFloat(), "Total Calorie Intake"),
            PieEntry(remainingCalorieLimit.toFloat(), "Remaining Calorie Limit")
        )

        // Create the pie chart data set and customize its appearance
        val dataSet = PieDataSet(entries, "")
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        // Set the colors for the pie chart slices
        val colors = mutableListOf<Int>()
        if (remainingCalorieLimit <= 0) {
            // Remaining sugar limit is 0 or negative, set color to red
            colors.add(ContextCompat.getColor(this, R.color.alert_color))
            colors.add(ContextCompat.getColor(this,R.color.soft_red))
        } else {
            // Remaining sugar limit is positive, set regular colors
            colors.add(ContextCompat.getColor(this, R.color.teal_700))
            colors.add(ContextCompat.getColor(this, R.color.teal_200))
        }
        dataSet.colors = colors

        // Create the pie chart and customize its appearance
        val pieChart: PieChart = findViewById(R.id.log_calorie_chart)
        pieChart.setUsePercentValues(false)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.dark_blue))

        // Set the data for the pie chart and refresh its display
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
        if (remainingCalorieLimit <= 0) {
            val textView: TextView = findViewById(R.id.calorie_intake_warning)
            textView.text = "DAILY CALORIE INTAKE EXCEEDED"
        }
    }


    private fun displayWeeklySugarLimitInPieChart(diabetesWeeklyLimits: DiabetesWeeklyLimit) {
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(diabetesWeeklyLimits.sugarLimit.toFloat(), ""))

        val dataSet = PieDataSet(entries, "Daily Sugar Intake Limit")
        dataSet.colors = listOf(Color.rgb(255,192,203))
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        data.setValueTextSize(16f)
        data.setValueTextColor(Color.BLACK)

        weeklySugarPieChart.data = data.apply {
            setValueFormatter(DoubleValueFormatter()) // Set custom value formatter
        }
        weeklySugarPieChart.invalidate()
    }
    private fun displayWeeklyCalorieLimitInPieChart(diabetesWeeklyLimits: DiabetesWeeklyLimit) {
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(diabetesWeeklyLimits.calorieLimit.toFloat(), ""))

        val dataSet = PieDataSet(entries, "Daily Calorie Intake Limit")
        dataSet.colors = listOf(Color.rgb(157, 190, 185))
        dataSet.valueTextColor = Color.BLACK


        val data = PieData(dataSet)
        data.setValueTextSize(16f)
        data.setValueTextColor(Color.BLACK)

        weeklyCaloriePieChart.data = data.apply {
            setValueFormatter(DoubleValueFormatter()) // Set custom value formatter
        }
        weeklyCaloriePieChart.invalidate()
    }
    private fun displayWeeklySugarIntakeInPieChart(totalSugar: Double, sugarLimit: Double) {
        // Calculate the remaining sugar limit
        val remainingSugarLimit = sugarLimit - totalSugar

        // Create the pie chart entries for total sugar intake and remaining sugar limit
        val entries = listOf(
            PieEntry(totalSugar.toFloat(), "Total Sugar Intake"),
            PieEntry(remainingSugarLimit.toFloat(), "Remaining Sugar Limit")
        )

        // Create the pie chart data set and customize its appearance
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.hard_pink),
            ContextCompat.getColor(this, R.color.soft_pink)
        )

        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        // Create the pie chart and customize its appearance
        val pieChart: PieChart = findViewById(R.id.weekly_sugar_chart)
        pieChart.setUsePercentValues(false)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.dark_blue))

        // Set the data for the pie chart and refresh its display
        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(DoubleValueFormatter()) // Set custom value formatter
        }
        pieChart.invalidate()
    }

    private fun displayWeeklyCalorieIntakeInPieChart(totalCalorie: Double, calorieLimit: Double) {
        // Calculate the remaining calorie limit
        val remainingCalorieLimit = calorieLimit - totalCalorie

        // Create the pie chart entries for total calorie intake and remaining calorie limit
        val entries = listOf(
            PieEntry(totalCalorie.toFloat(), "Total Calorie Intake"),
            PieEntry(remainingCalorieLimit.toFloat(), "Remaining Calorie Limit")
        )

        // Create the pie chart data set and customize its appearance
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.teal_700),
            ContextCompat.getColor(this, R.color.teal_200)
        )

        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        // Create the pie chart and customize its appearance
        val pieChart: PieChart = findViewById(R.id.weekly_calorie_chart)
        pieChart.setUsePercentValues(false)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.dark_blue))

        // Set the data for the pie chart and refresh its display
        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(DoubleValueFormatter()) // Set custom value formatter
        }
        pieChart.invalidate()
    }

    private fun displayMonthlySugarLimitInPieChart(diabetesMonthlyLimits: DiabetesMonthlyLimit) {
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(diabetesMonthlyLimits.sugarLimit.toFloat(), ""))

        val dataSet = PieDataSet(entries, "Daily Sugar Intake Limit")
        dataSet.colors = listOf(Color.rgb(255,192,203))
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        data.setValueTextSize(16f)
        data.setValueTextColor(Color.BLACK)

        monthlySugarPieChart.data = data.apply {
            setValueFormatter(DoubleValueFormatter()) // Set custom value formatter
        }
        monthlySugarPieChart.invalidate()
    }
    private fun displayMonthlyCalorieLimitInPieChart(diabetesMonthlyLimits: DiabetesMonthlyLimit) {
        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(diabetesMonthlyLimits.calorieLimit.toFloat(), ""))

        val dataSet = PieDataSet(entries, "Daily Calorie Intake Limit")
        dataSet.colors = listOf(Color.rgb(157, 190, 185))
        dataSet.valueTextColor = Color.BLACK


        val data = PieData(dataSet)
        data.setValueTextSize(16f)
        data.setValueTextColor(Color.BLACK)

        monthlyCaloriePieChart.data = data.apply {
            setValueFormatter(DoubleValueFormatter()) // Set custom value formatter
        }
        monthlyCaloriePieChart.invalidate()
    }

    private fun displayMonthlySugarIntakeInPieChart(totalSugar: Double, sugarLimit: Double) {
        // Calculate the remaining sugar limit
        val remainingSugarLimit = sugarLimit - totalSugar

        // Create the pie chart entries for total sugar intake and remaining sugar limit
        val entries = listOf(
            PieEntry(totalSugar.toFloat(), "Total Sugar Intake"),
            PieEntry(remainingSugarLimit.toFloat(), "Remaining Sugar Limit")
        )

        // Create the pie chart data set and customize its appearance
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.hard_pink),
            ContextCompat.getColor(this, R.color.soft_pink)
        )

        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        // Create the pie chart and customize its appearance
        val pieChart: PieChart = findViewById(R.id.monthly_sugar_chart)
        pieChart.setUsePercentValues(false)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.dark_blue))

        // Set the data for the pie chart and refresh its display
        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(DoubleValueFormatter()) // Set custom value formatter
        }
        pieChart.invalidate()
    }

    private fun displayMonthlyCalorieIntakeInPieChart(totalCalorie: Double, calorieLimit: Double) {
        // Calculate the remaining calorie limit
        val remainingCalorieLimit = calorieLimit - totalCalorie

        // Create the pie chart entries for total calorie intake and remaining calorie limit
        val entries = listOf(
            PieEntry(totalCalorie.toFloat(), "Total Calorie Intake"),
            PieEntry(remainingCalorieLimit.toFloat(), "Remaining Calorie Limit")
        )

        // Create the pie chart data set and customize its appearance
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.teal_700),
            ContextCompat.getColor(this, R.color.teal_200)
        )

        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        // Create the pie chart and customize its appearance
        val pieChart: PieChart = findViewById(R.id.monthly_calorie_chart)
        pieChart.setUsePercentValues(false)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.legend.isEnabled = false
        pieChart.description.isEnabled = false
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.dark_blue))

        // Set the data for the pie chart and refresh its display
        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(DoubleValueFormatter()) // Set custom value formatter
        }
        pieChart.invalidate()
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

    class DoubleValueFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format("%.1f", value.toDouble())
        }
    }

}
