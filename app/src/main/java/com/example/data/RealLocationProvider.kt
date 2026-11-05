package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

data class UserRealLocation(
    val latitude: Double = 48.8566,
    val longitude: Double = 2.3522,
    val accuracy: Float = 10f,
    val speed: Float = 0f,
    val address: String = "Paris, France",
    val isRealGpsAcquired: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

class RealLocationProvider(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _locationFlow = MutableStateFlow(UserRealLocation())
    val locationFlow: StateFlow<UserRealLocation> = _locationFlow.asStateFlow()

    private val locationListener = AppLocationListener()

    private inner class AppLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleNewLocation(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationManager == null) return

        try {
            // Check last known location from GPS and Network
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLast = when {
                lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                lastNet != null -> lastNet
                else -> null
            }
            if (bestLast != null) {
                handleNewLocation(bestLast)
            }

            // Request updates from GPS provider
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L, // every 3 seconds
                    2.0f,  // or 2 meters
                    locationListener,
                    Looper.getMainLooper()
                )
            }

            // Request updates from Network provider
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    2.0f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopLocationUpdates() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleNewLocation(loc: Location) {
        scope.launch {
            var addressText = "Position GPS (${loc.latitude.format(4)}, ${loc.longitude.format(4)})"
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { addresses ->
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                val street = addr.thoroughfare ?: addr.subLocality ?: ""
                                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Inconnu"
                                addressText = if (street.isNotBlank()) "$street, $city" else city
                                updateState(loc, addressText)
                            }
                        }
                        return@launch
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val street = addr.thoroughfare ?: addr.subLocality ?: ""
                            val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Inconnu"
                            addressText = if (street.isNotBlank()) "$street, $city" else city
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to coordinates
            }
            updateState(loc, addressText)
        }
    }

    private fun updateState(loc: Location, address: String) {
        _locationFlow.value = UserRealLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracy = loc.accuracy,
            speed = loc.speed,
            address = address,
            isRealGpsAcquired = true,
            lastUpdated = System.currentTimeMillis()
        )

        // Relocate friends relative to user's real geographic location so they are genuinely near the user on the map!
        FriendsLocationService.anchorFriendsAroundUser(loc.latitude, loc.longitude)
    }

    companion object {
        fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0 // Earth radius in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val dist = r * c
            return Math.round(dist * 100.0) / 100.0
        }
    }
}

private fun Double.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)
