package com.tagalert.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Helper for getting the device's current location.
 * Uses Google Play Services Fused Location Provider.
 */
@Singleton
class LocationHelper @Inject constructor(
    private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Gets the last known location quickly, or requests a fresh one.
     * Returns null if permissions aren't granted.
     */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        // Try last known first
        val lastKnown = getLastKnownLocation()
        if (lastKnown != null && (System.currentTimeMillis() - lastKnown.time) < 30_000) {
            return lastKnown
        }

        // Request fresh location
        return requestFreshLocation()
    }

    private suspend fun getLastKnownLocation(): Location? {
        return try {
            suspendCancellableCoroutine { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        } catch (e: SecurityException) {
            null
        }
    }

    private suspend fun requestFreshLocation(): Location? {
        return try {
            suspendCancellableCoroutine { cont ->
                val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000)
                    .setMinUpdateIntervalMillis(2000)
                    .setMaxUpdates(1)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedClient.removeLocationUpdates(this)
                        if (cont.isActive) cont.resume(result.lastLocation)
                    }
                }

                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

                // Timeout after 10 seconds
                cont.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                }
            }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
