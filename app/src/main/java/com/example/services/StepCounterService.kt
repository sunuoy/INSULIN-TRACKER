package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.database.AppDatabase
import com.example.data.model.StepCountRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.sqrt

class StepCounterService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "StepCounterService"

        private val _liveStepsFlow = MutableStateFlow(0)
        val liveStepsFlow: StateFlow<Int> = _liveStepsFlow.asStateFlow()

        private val _isSensorActiveFlow = MutableStateFlow(false)
        val isSensorActiveFlow: StateFlow<Boolean> = _isSensorActiveFlow.asStateFlow()

        private val _activeSensorTypeFlow = MutableStateFlow("None")
        val activeSensorTypeFlow: StateFlow<String> = _activeSensorTypeFlow.asStateFlow()
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var sensorManager: SensorManager
    private var stepDetectorSensor: Sensor? = null
    private var stepCounterSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private lateinit var database: AppDatabase

    // Accelerometer dynamic peak detection variables
    private var lastStepTimeNs: Long = 0
    private val gravity = FloatArray(3)
    private val alpha = 0.82f // Low-pass filter for gravity isolation
    private var movingAvgMagnitude = 9.8f
    private val MIN_STEP_DELAY_NS = 260_000_000L // 260ms lockout (max ~3.8 steps/sec)
    private val PEAK_THRESHOLD = 2.2f // Dynamic peak acceleration threshold in m/s^2
    private var isAboveThreshold = false

    private val CHANNEL_ID = "step_tracker_channel"
    private val NOTIFICATION_ID = 888

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StepCounterService onCreate called")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        database = AppDatabase.getDatabase(applicationContext)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0))

        // Initial sync of today's total steps from DB into live flow
        scope.launch {
            syncTodayStepsFromDatabase()
        }

        // Register best available step sensor:
        // Priority 1: Hardware Step Detector (Instantaneous 1-step event callback)
        // Priority 2: Hardware Step Counter (Cumulative delta calculation)
        // Priority 3: Real-time Accelerometer with Dynamic Peak Filter
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var registered = false
        if (stepDetectorSensor != null) {
            registered = sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_GAME)
            if (registered) {
                _activeSensorTypeFlow.value = "Hardware Step Detector (Instant)"
                Log.d(TAG, "Hardware Step Detector registered with SENSOR_DELAY_GAME")
            }
        }

        if (!registered && stepCounterSensor != null) {
            registered = sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            if (registered) {
                _activeSensorTypeFlow.value = "Hardware Step Counter (Cumulative)"
                Log.d(TAG, "Hardware Step Counter registered")
            }
        }

        if (!registered && accelSensor != null) {
            registered = sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            if (registered) {
                _activeSensorTypeFlow.value = "Accelerometer (Dynamic Peak Cadence)"
                Log.d(TAG, "Accelerometer registered with dynamic peak detection")
            }
        }

        _isSensorActiveFlow.value = registered
        if (!registered) {
            Log.e(TAG, "No motion sensors available on this device")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "StepCounterService onStartCommand called")
        scope.launch {
            syncTodayStepsFromDatabase()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            // 1. Hardware Step Detector (Real-time single step event)
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values.isNotEmpty() && event.values[0] == 1.0f) {
                    onStepDetected(1)
                }
            }

            // 2. Hardware Cumulative Step Counter
            Sensor.TYPE_STEP_COUNTER -> {
                val currentSensorSteps = event.values[0].toInt()
                scope.launch {
                    processCumulativeSensorSteps(currentSensorSteps)
                }
            }

            // 3. Accelerometer Dynamic Peak Filter
            Sensor.TYPE_ACCELEROMETER -> {
                processAccelerometerEvent(event)
            }
        }
    }

    private fun processAccelerometerEvent(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 1. Low-pass filter to isolate gravity
        gravity[0] = alpha * gravity[0] + (1 - alpha) * x
        gravity[1] = alpha * gravity[1] + (1 - alpha) * y
        gravity[2] = alpha * gravity[2] + (1 - alpha) * z

        // 2. High-pass filter to isolate pure user motion acceleration
        val linearX = x - gravity[0]
        val linearY = y - gravity[1]
        val linearZ = z - gravity[2]

        val linearMagnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        // 3. Dynamic peak crossing detection with refractory lockout
        val nowNs = event.timestamp
        if (linearMagnitude > PEAK_THRESHOLD) {
            if (!isAboveThreshold && (nowNs - lastStepTimeNs) > MIN_STEP_DELAY_NS) {
                lastStepTimeNs = nowNs
                isAboveThreshold = true
                onStepDetected(1)
            }
        } else if (linearMagnitude < PEAK_THRESHOLD * 0.6f) {
            isAboveThreshold = false
        }
    }

    /**
     * Called whenever a verified physical step is detected.
     * Updates in-memory real-time flow (0ms UI lag) and persists to Room DB.
     */
    private fun onStepDetected(stepCount: Int) {
        _liveStepsFlow.value += stepCount
        scope.launch {
            persistStepsToDatabase(stepCount)
        }
    }

    private suspend fun persistStepsToDatabase(count: Int) {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

        val allRecords = database.stepDao().getAllStepRecords().firstOrNull() ?: emptyList()
        val todayRecord = allRecords.find { record ->
            val recCal = Calendar.getInstance().apply { timeInMillis = record.dateTimeMillis }
            recCal.get(Calendar.YEAR) == todayYear && recCal.get(Calendar.DAY_OF_YEAR) == todayDay
        }

        val totalToday: Int
        if (todayRecord != null) {
            totalToday = todayRecord.steps + count
            val updatedRecord = todayRecord.copy(
                steps = totalToday,
                dateTimeMillis = System.currentTimeMillis()
            )
            database.stepDao().insertStepRecord(updatedRecord)
        } else {
            totalToday = count
            val newRecord = StepCountRecord(
                steps = count,
                dateTimeMillis = System.currentTimeMillis()
            )
            database.stepDao().insertStepRecord(newRecord)
        }

        _liveStepsFlow.value = totalToday
        updateNotification(totalToday)
    }

    private suspend fun processCumulativeSensorSteps(currentSensorSteps: Int) {
        val sharedPrefs = getSharedPreferences("step_tracker_prefs", Context.MODE_PRIVATE)
        val lastSensorSteps = sharedPrefs.getInt("last_sensor_steps", -1)

        if (lastSensorSteps == -1 || currentSensorSteps < lastSensorSteps) {
            sharedPrefs.edit().putInt("last_sensor_steps", currentSensorSteps).apply()
            return
        }

        val diff = currentSensorSteps - lastSensorSteps
        if (diff > 0) {
            sharedPrefs.edit().putInt("last_sensor_steps", currentSensorSteps).apply()
            persistStepsToDatabase(diff)
        }
    }

    private suspend fun syncTodayStepsFromDatabase() {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

        val allRecords = database.stepDao().getAllStepRecords().firstOrNull() ?: emptyList()
        val todaySteps = allRecords.filter { record ->
            val recCal = Calendar.getInstance().apply { timeInMillis = record.dateTimeMillis }
            recCal.get(Calendar.YEAR) == todayYear && recCal.get(Calendar.DAY_OF_YEAR) == todayDay
        }.sumOf { it.steps }

        _liveStepsFlow.value = todaySteps
        updateNotification(todaySteps)
    }

    private fun updateNotification(steps: Int) {
        scope.launch {
            val profile = database.profileDao().getProfileSync()
            val stepGoal = if (profile != null && profile.stepGoal > 0) profile.stepGoal else 10000

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildNotification(steps, stepGoal))
        }
    }

    private fun buildNotification(steps: Int, goal: Int = 10000): Notification {
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val progressPct = ((steps.toFloat() / goal) * 100).toInt().coerceIn(0, 100)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Real-time Pedometer • $steps / $goal steps")
            .setContentText("Walking: $progressPct% of daily target • Live Tracking")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setProgress(goal, steps.coerceAtMost(goal), false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Real-time Step Tracker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live real-time fitness step monitoring"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        Log.d(TAG, "StepCounterService onDestroy called")
        sensorManager.unregisterListener(this)
        _isSensorActiveFlow.value = false
        _activeSensorTypeFlow.value = "None"
        job.cancel()
        super.onDestroy()
    }
}
