package com.example.WeatherApps

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var apiKey: String

    private val weatherIcons = mapOf(
        "Clear" to R.drawable.weatherclear,
        "Clouds" to R.drawable.weathercloudy,
        "Rain" to R.drawable.weatherrainy,
        "Snow" to R.drawable.weathersnowy,
        "Thunderstorm" to R.drawable.weatherthunderstorm,
        "Drizzle" to R.drawable.weatherdrizzle,
        "Mist" to R.drawable.weathermist
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        val mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
        val weatherIconImageView = findViewById<ImageView>(R.id.currentWeatherImageView)
        val cityEditText = findViewById<EditText>(R.id.cityEditText)
        val getWeatherButton = findViewById<Button>(R.id.getWeatherButton)
        val weatherTextView = findViewById<TextView>(R.id.weatherTextView)
        val dailyWeatherTextView = findViewById<TextView>(R.id.dailyWeatherTextView)
        val weeklyWeatherTextView = findViewById<TextView>(R.id.weeklyWeatherTextView)

        try {
            // Load API key from local.properties
            val localProperties = Properties()
            assets.open("local.properties").use { propertiesFile ->
                localProperties.load(propertiesFile)
                apiKey = localProperties.getProperty("apiKey", "82d308e1ae68c22d51869f38d30912d2")
            }
        } catch (e: IOException) {
            Log.e("API_KEY", "Failed to load API key from local.properties", e)
            apiKey = "82d308e1ae68c22d51869f38d30912d2" // Fallback to hardcoded API key
        }

        getWeatherButton.setOnClickListener {
            val city = cityEditText.text.toString()
            getWeather(city, weatherTextView, weatherIconImageView, mainLayout)
            getDailyWeather(city, dailyWeatherTextView)
            getWeeklyWeather(city, weeklyWeatherTextView)
        }
    }

    private fun getWeather(
        city: String,
        weatherTextView: TextView,
        weatherIconImageView: ImageView,
        mainLayout: RelativeLayout
    ) {
        val url =
            "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("API_CALL", "Failed to retrieve weather data", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        val json = JSONObject(responseData)
                        val weatherArray = json.getJSONArray("weather")
                        val weather = weatherArray.getJSONObject(0)
                        val description = weather.getString("main")
                        val temp = json.getJSONObject("main").getDouble("temp")

                        runOnUiThread {
                            weatherTextView.text = "Temperature: $temp°C\nWeather: $description"

                            // Update weather icon
                            updateWeatherIcon(description, weatherIconImageView)

                            // Update background based on weather condition
                            updateBackground(description, mainLayout)
                        }
                    }
                }
            }
        })
    }

    private fun updateWeatherIcon(weatherCondition: String, weatherIconImageView: ImageView) {
        val iconResId = weatherIcons[weatherCondition]
        if (iconResId != null) {
            weatherIconImageView.setImageResource(iconResId)
        }
    }

    private fun updateBackground(weatherCondition: String, mainLayout: RelativeLayout) {
        val backgroundResId = when (weatherCondition) {
            "Clear" -> R.drawable.backgroundsunny
            "Clouds" -> R.drawable.backgroundcloudy
            "Rain" -> R.drawable.backgroundrainy
            "Mist" -> R.drawable.backgroundmist
            "Thunderstorm" -> R.drawable.backgroundthunderstorm
            "Drizzle" -> R.drawable.backgrounddrizzle
            "Snow" -> R.drawable.backgroundsnowy
            else -> R.drawable.backgroundclear // Default background
        }
        mainLayout.setBackgroundResource(backgroundResId)
    }

    private fun getDailyWeather(city: String, dailyWeatherTextView: TextView) {
        val url =
            "https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$apiKey&units=metric"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("API_CALL", "Failed to retrieve daily weather data", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        val json = JSONObject(responseData)
                        val list = json.getJSONArray("list")
                        val dailyWeather = StringBuilder()

                        // Only consider the next 24 hours (4 x 6-hour intervals)
                        val hoursCount = minOf(list.length(), 4)
                        for (i in 0 until hoursCount) {
                            val day = list.getJSONObject(i)
                            val temp = day.getJSONObject("main").getDouble("temp")
                            val weatherArray = day.getJSONArray("weather")
                            val weather = weatherArray.getJSONObject(0)
                            val description = weather.getString("main")

                            dailyWeather.append("Hour ${i * 6}: $temp°C, $description\n")
                        }

                        runOnUiThread {
                            dailyWeatherTextView.text = dailyWeather.toString()
                        }
                    }
                }
            }
        })
    }

    private fun getWeeklyWeather(city: String, weeklyWeatherTextView: TextView) {
        val url = "http://api.openweathermap.org/data/2.5/forecast/daily?q=$city&cnt=7&appid=$apiKey&units=metric"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("API_CALL", "Failed to retrieve weekly weather data", e)
                runOnUiThread {
                    weeklyWeatherTextView.text = "Failed to retrieve weekly weather data"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val json = JSONObject(responseData)
                        val dailyArray = json.getJSONArray("list")
                        val weeklyWeather = StringBuilder()

                        // Ensure we only loop for 7 days or less if data is insufficient
                        val daysCount = minOf(dailyArray.length(), 7)
                        for (i in 0 until daysCount) {
                            val day = dailyArray.getJSONObject(i)
                            val temp = day.getJSONObject("temp").getDouble("day")
                            val weatherArray = day.getJSONArray("weather")
                            val weather = weatherArray.getJSONObject(0)
                            val description = weather.getString("main")

                            weeklyWeather.append("Day ${i + 1}: $temp°C, $description\n")
                        }

                        runOnUiThread {
                            weeklyWeatherTextView.text = weeklyWeather.toString()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e("JSON_PARSE", "Error parsing JSON response", e)
                        runOnUiThread {
                            weeklyWeatherTextView.text = "Error parsing JSON"
                        }
                    }
                } else {
                    Log.e("RESPONSE_CODE", "Failed to fetch data: ${response.code}")
                    runOnUiThread {
                        weeklyWeatherTextView.text = "Failed to fetch data: ${response.code}"
                    }
                }
            }
        })
    }
}
