package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MockLocationService : Service() {

    private lateinit var locationManager: LocationManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isMockingActive = false

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "mock_location_service_channel"

        const val ACTION_START = "START_MOCK"
        const val ACTION_STOP = "STOP_MOCK"

        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_COUNTRY = "extra_country"

        // Reactive states for UI bindings
        val isRunning = MutableStateFlow(false)
        val spoofedCountry = MutableStateFlow<String?>(null)
        val spoofedLatitude = MutableStateFlow<Double?>(null)
        val spoofedLongitude = MutableStateFlow<Double?>(null)
        val securityExceptionOccurred = MutableStateFlow(false)
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_STOP

        if (action == ACTION_STOP) {
            stopMocking()
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START && intent != null) {
            val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
            val lng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
            val country = intent.getStringExtra(EXTRA_COUNTRY) ?: "Custom Location"

            startMocking(lat, lng, country)
        }

        return START_STICKY
    }

    private fun startMocking(lat: Double, lng: Double, country: String) {
        securityExceptionOccurred.value = false
        spoofedCountry.value = country
        spoofedLatitude.value = lat
        spoofedLongitude.value = lng

        createNotificationChannel()
        val notification = buildNotification(country, lat, lng)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // Fallback for newer Android exceptions in foreground service launching
            startForeground(NOTIFICATION_ID, notification)
        }

        if (!isMockingActive) {
            isMockingActive = true
            isRunning.value = true

            // Initialize test providers
            registerTestProvider(LocationManager.GPS_PROVIDER)
            registerTestProvider(LocationManager.NETWORK_PROVIDER)

            // Start loop to continuously mock location
            serviceScope.launch {
                while (isMockingActive) {
                    setMockLocation(LocationManager.GPS_PROVIDER, lat, lng)
                    setMockLocation(LocationManager.NETWORK_PROVIDER, lat, lng)
                    delay(1500) // update every 1.5 seconds
                }
            }
        }
    }

    private fun stopMocking() {
        isMockingActive = false
        isRunning.value = false
        spoofedCountry.value = null
        spoofedLatitude.value = null
        spoofedLongitude.value = null

        // Remove test providers
        unregisterTestProvider(LocationManager.GPS_PROVIDER)
        unregisterTestProvider(LocationManager.NETWORK_PROVIDER)
    }

    private fun registerTestProvider(providerName: String) {
        try {
            // First, try removing if already exists to clear stale state
            try {
                locationManager.removeTestProvider(providerName)
            } catch (e: Exception) {
                // Ignore
            }

            locationManager.addTestProvider(
                providerName,
                false, // requiresNetwork
                false, // requiresSatellite
                false, // requiresCell
                false, // hasMonetaryCost
                true,  // supportsAltitude
                true,  // supportsSpeed
                true,  // supportsBearing
                Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(providerName, true)
        } catch (e: SecurityException) {
            securityExceptionOccurred.value = true
            stopMocking()
            stopSelf()
        } catch (e: Exception) {
            // Ignore other registration exceptions
        }
    }

    private fun unregisterTestProvider(providerName: String) {
        try {
            locationManager.removeTestProvider(providerName)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun setMockLocation(providerName: String, lat: Double, lng: Double) {
        if (!isMockingActive) return
        try {
            val mockLocation = Location(providerName).apply {
                latitude = lat
                longitude = lng
                altitude = 12.0
                time = System.currentTimeMillis()
                accuracy = 1.0f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
                speed = 0.0f
                bearing = 0.0f
            }
            locationManager.setTestProviderLocation(providerName, mockLocation)
        } catch (e: SecurityException) {
            securityExceptionOccurred.value = true
            stopMocking()
            stopSelf()
        } catch (e: Exception) {
            // Ignore other injection exceptions
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "GPS Spoofer Active Location"
            val descriptionText = "Shows persistent notifications when location spoofing is active"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(country: String, lat: Double, lng: Double): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, MockLocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val formattedCoords = String.format("%.4f, %.4f", lat, lng)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("تغيير الموقع نشط | Location Spoofing Active")
            .setContentText("الموقع الحالي: $country ($formattedCoords)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "إيقاف التشغيل | STOP",
                stopPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText("تطبيق GPS Spoofer يقوم بمحاكاة موقعك في: \nالدولة: $country\nالإحداثيات: $formattedCoords\nيمكنك فتح المواقع والخدمات الآن."))
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMocking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
