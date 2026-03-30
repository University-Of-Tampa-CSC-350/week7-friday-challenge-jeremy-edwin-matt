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
    // Adjusted for a responsive, real-time feel (Task 3 & 5)
    private val sensitivity = 2.5f

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

        // Task 6: Handle Sensor Availability
        if (accelerometer == null) {
            tvValues.text = "Accelerometer not available on this device."
            ball.visibility = View.GONE
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Using SENSOR_DELAY_GAME for high-frequency, real-time updates (Task 5)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        // Task 7: Lifecycle management
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Raw accelerometer values
            val sensorX = event.values[0]
            val sensorY = event.values[1]

            // Task 5: Real-Time Updates
            // We update the UI every time the sensor reports a change.
            // Using SENSOR_DELAY_GAME ensures minimal lag.

            // Display values for debugging and "Room" info (Task 4 trail and error)
            tvValues.text = String.format("X: %.2f, Y: %.2f\nRoom: %d x %d", 
                sensorX, sensorY, container.width, container.height)

            // Calculate movement delta
            // Negate X because tilting right (negative sensorX) should move ball right (positive screenX)
            val deltaX = -sensorX * sensitivity
            val deltaY = sensorY * sensitivity

            // New target positions
            var nextX = ball.x + deltaX
            var nextY = ball.y + deltaY

            // Ensure container dimensions are available before clamping
            if (container.width > 0 && container.height > 0) {
                // Task 4: Maintain Screen Boundaries
                val maxX = (container.width - ball.width).toFloat()
                val maxY = (container.height - ball.height).toFloat()

                // Clamp values to stay within the visible area
                nextX = nextX.coerceIn(0f, maxX)
                nextY = nextY.coerceIn(0f, maxY)

                // Task 5: Apply position updates immediately to reflect live input
                ball.x = nextX
                ball.y = nextY
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
}
