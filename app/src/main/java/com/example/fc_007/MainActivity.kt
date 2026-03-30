package com.example.fc_007

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    private lateinit var ball: View
    private lateinit var container: FrameLayout
    private lateinit var tvValues: TextView

    // Sensitivity factor to scale raw accelerometer values
    private val sensitivity = 2.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ball = findViewById(R.id.ball)
        container = findViewById(R.id.container)
        tvValues = findViewById(R.id.tvValues)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // X-axis: tilting left makes X positive, right makes X negative
            // Y-axis: tilting forward (away) makes Y positive, backward (toward) makes Y negative
            // Note: On Android, X is positive to the left, Y is positive downwards (if held upright)
            // But usually we want: tilt right -> move right (positive X in screen coords)
            // tilt down -> move down (positive Y in screen coords)
            
            val sensorX = event.values[0]
            val sensorY = event.values[1]

            // Display raw values (Bonus)
            tvValues.text = String.format("X: %.2f, Y: %.2f", sensorX, sensorY)

            // Calculate new position
            // We use -sensorX because tilting right (positive screen X) results in negative sensor X
            // We use sensorY because tilting down (positive screen Y) results in positive sensor Y (when held upright)
            // However, it depends on device orientation. For a simple implementation:
            var newX = ball.x - (sensorX * sensitivity)
            var newY = ball.y + (sensorY * sensitivity)

            // Task 4: Maintain Screen Boundaries (Basic implementation)
            // Boundary checks
            if (newX < 0) newX = 0f
            if (newX > container.width - ball.width) newX = (container.width - ball.width).toFloat()
            
            if (newY < 0) newY = 0f
            if (newY > container.height - ball.height) newY = (container.height - ball.height).toFloat()

            // Update position (Task 5: Real-Time Updates)
            ball.x = newX
            ball.y = newY
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}