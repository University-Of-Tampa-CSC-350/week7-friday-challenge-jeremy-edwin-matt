package com.example.fc_007

import android.content.Context
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.LinkedList
import kotlin.random.Random

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    private lateinit var ball: View
    private lateinit var container: FrameLayout
    private lateinit var tvValues: TextView
    private lateinit var tvScore: TextView
    private lateinit var dotGreen: View
    
    private val redDots = mutableListOf<RedDot>()
    private val maxRedDots = 5
    
    private var score = 0
    private val sensitivity = 4.0f
    
    private var smoothX = 0f
    private var smoothY = 0f
    private val alpha = 0.1f 

    private var isInitialized = false
    private var isMovingReds = false

    // Size constraints for the ball
    private val minBallSize = 40
    private val maxBallSize = 250
    private var currentBallSize = 100

    data class RedDot(val view: View, var speedX: Float = 0f, var speedY: Float = 0f, val trail: LinkedList<View> = LinkedList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ball = findViewById(R.id.ball)
        container = findViewById(R.id.container)
        tvValues = findViewById(R.id.tvValues)
        tvScore = findViewById(R.id.tvScore)
        dotGreen = findViewById(R.id.dotGreen)

        updateBallSize(currentBallSize)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            tvValues.text = "Error: Accelerometer not found!"
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_LONG).show()
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
            smoothX = alpha * event.values[0] + (1 - alpha) * smoothX
            smoothY = alpha * event.values[1] + (1 - alpha) * smoothY

            tvValues.text = String.format("X: %.2f, Y: %.2f", smoothX, smoothY)

            if (!isInitialized && container.width > 0) {
                ball.x = (container.width / 2 - ball.width / 2).toFloat()
                ball.y = (container.height / 2 - ball.height / 2).toFloat()
                
                spawnDot(dotGreen)
                dotGreen.visibility = View.VISIBLE
                addNewRedDot()
                
                isInitialized = true
            }

            if (isInitialized) {
                moveBall(-smoothX, smoothY)
                
                if (score >= 50) {
                    isMovingReds = true
                } else if (score < 45) {
                    isMovingReds = false
                }
                
                if (isMovingReds) {
                    moveRedDots()
                } else {
                    // Update trails even if not moving (they should fade/disappear if static)
                    updateTrails()
                }
                
                checkCollisions()
                adjustRedDotCount()
            }
        }
    }

    private fun moveBall(deltaX: Float, deltaY: Float) {
        var newX = ball.x + (deltaX * sensitivity)
        var newY = ball.y + (deltaY * sensitivity)

        val maxX = (container.width - ball.width).toFloat()
        val maxY = (container.height - ball.height).toFloat()

        if (newX < 0) newX = 0f
        if (newX > maxX) newX = maxX
        
        if (newY < 0) newY = 0f
        if (newY > maxY) newY = maxY

        ball.x = newX
        ball.y = newY
    }

    private fun spawnDot(dotView: View) {
        val dotWidth = if (dotView.width > 0) dotView.width else dotView.layoutParams.width
        val dotHeight = if (dotView.height > 0) dotView.height else dotView.layoutParams.height
        
        val maxX = (container.width - dotWidth).toFloat()
        val maxY = (container.height - dotHeight).toFloat()
        
        dotView.x = Random.nextFloat() * maxX
        dotView.y = Random.nextFloat() * maxY
    }

    private fun addNewRedDot() {
        if (redDots.size >= maxRedDots) return
        
        val newRedView = View(this)
        val dotSize = if (dotGreen.width > 0) dotGreen.width else 90
        newRedView.layoutParams = ViewGroup.LayoutParams(dotSize, dotSize)
        newRedView.background = getDrawable(R.drawable.red_dot_shape)
        newRedView.elevation = dotGreen.elevation
        
        container.addView(newRedView)
        spawnDot(newRedView)
        
        // 1. Move them a bit slower (0.3f to 1.0f)
        val speedX = (Random.nextFloat() * 0.7f + 0.3f) * if (Random.nextBoolean()) 1 else -1
        val speedY = (Random.nextFloat() * 0.7f + 0.3f) * if (Random.nextBoolean()) 1 else -1
        
        redDots.add(RedDot(newRedView, speedX, speedY))
    }

    private fun updateTrails() {
        for (dot in redDots) {
            // Add current position to trail
            val trailPiece = View(this)
            trailPiece.layoutParams = ViewGroup.LayoutParams(dot.view.width, dot.view.height)
            trailPiece.background = getDrawable(R.drawable.red_dot_shape)
            trailPiece.alpha = 0.5f
            trailPiece.x = dot.view.x
            trailPiece.y = dot.view.y
            trailPiece.elevation = dot.view.elevation - 1
            
            container.addView(trailPiece)
            dot.trail.addFirst(trailPiece)
            
            // Limit trail length
            if (dot.trail.size > 10) {
                val oldPiece = dot.trail.removeLast()
                container.removeView(oldPiece)
            }
            
            // Fade existing pieces
            for (i in dot.trail.indices) {
                dot.trail[i].alpha = 0.5f * (1f - i.toFloat() / 10f)
            }
        }
    }

    private fun moveRedDots() {
        updateTrails()
        for (dot in redDots) {
            val dotWidth = if (dot.view.width > 0) dot.view.width else dot.view.layoutParams.width
            val dotHeight = if (dot.view.height > 0) dot.view.height else dot.view.layoutParams.height
            
            var nextX = dot.view.x + dot.speedX
            var nextY = dot.view.y + dot.speedY

            // 3. Make it so the red balls bounce off the border walls (Implemented via speed inversion)
            if (nextX <= 0) {
                dot.speedX = Math.abs(dot.speedX)
                nextX = 0f
            } else if (nextX >= container.width - dotWidth) {
                dot.speedX = -Math.abs(dot.speedX)
                nextX = (container.width - dotWidth).toFloat()
            }
            
            if (nextY <= 0) {
                dot.speedY = Math.abs(dot.speedY)
                nextY = 0f
            } else if (nextY >= container.height - dotHeight) {
                dot.speedY = -Math.abs(dot.speedY)
                nextY = (container.height - dotHeight).toFloat()
            }

            dot.view.x = nextX
            dot.view.y = nextY
        }
    }

    private fun adjustRedDotCount() {
        val desiredRedDots = minOf(1 + (score / 10), maxRedDots)
        while (redDots.size < desiredRedDots) {
            addNewRedDot()
        }
    }

    private fun checkCollisions() {
        val padding = ball.width * 0.1f
        val ballRect = RectF(ball.x + padding, ball.y + padding, ball.x + ball.width - padding, ball.y + ball.height - padding)
        
        val greenRect = RectF(dotGreen.x, dotGreen.y, dotGreen.x + dotGreen.width, dotGreen.y + dotGreen.height)
        if (RectF.intersects(ballRect, greenRect)) {
            // 4. Gain 5 points per green when you get to 50 score
            score += if (score >= 50) 5 else 2
            changeSize(20)
            spawnDot(dotGreen)
            updateScoreDisplay()
        }

        val iterator = redDots.iterator()
        while (iterator.hasNext()) {
            val redDot = iterator.next()
            val redDotWidth = if (redDot.view.width > 0) redDot.view.width else redDot.view.layoutParams.width
            val redDotHeight = if (redDot.view.height > 0) redDot.view.height else redDot.view.layoutParams.height
            
            val redRect = RectF(redDot.view.x, redDot.view.y, redDot.view.x + redDotWidth, redDot.view.y + redDotHeight)
            if (RectF.intersects(ballRect, redRect)) {
                // 4. Lose 3 points when hitting a red past 50 score
                score = if (score >= 50) maxOf(0, score - 3) else maxOf(0, score - 5)
                changeSize(-20)
                spawnDot(redDot.view)
                updateScoreDisplay()
            }
        }
    }

    private fun changeSize(delta: Int) {
        val newSize = currentBallSize + delta
        val maxPossibleSize = minOf(container.width, container.height, maxBallSize)
        if (newSize in minBallSize..maxPossibleSize) {
            currentBallSize = newSize
            updateBallSize(currentBallSize)
            moveBall(0f, 0f)
        }
    }

    private fun updateBallSize(size: Int) {
        val params = ball.layoutParams
        params.width = size
        params.height = size
        ball.layoutParams = params
    }

    private fun updateScoreDisplay() {
        tvScore.text = "Score: $score"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}