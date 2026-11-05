package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.data.model.GpsLocationData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.random.Random

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Base coordinates fallback (e.g. San Francisco Tech District or simulated lab)
    private var baseLat = 37.774929
    private var baseLng = -122.419416

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    suspend fun getCurrentLocation(): GpsLocationData {
        if (!hasLocationPermission()) {
            return getSimulatedLocation(isReal = false)
        }

        return try {
            val location = fetchGmsLocation()
            if (location != null) {
                GpsLocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = if (location.hasAltitude()) location.altitude else 14.5,
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else 3.8f,
                    timestamp = location.time,
                    isRealGps = true,
                    provider = location.provider ?: "FusedGPS"
                )
            } else {
                getSimulatedLocation(isReal = false)
            }
        } catch (e: Exception) {
            getSimulatedLocation(isReal = false)
        }
    }

    private suspend fun fetchGmsLocation(): Location? = suspendCancellableCoroutine { cont ->
        try {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { location ->
                cont.resume(location)
            }.addOnFailureListener {
                cont.resume(null)
            }
            cont.invokeOnCancellation {
                cts.cancel()
            }
        } catch (e: SecurityException) {
            cont.resume(null)
        } catch (e: Exception) {
            cont.resume(null)
        }
    }

    fun getSimulatedLocation(isReal: Boolean = false): GpsLocationData {
        // Small realistic micro-jitter
        val latJitter = (Random.nextDouble() - 0.5) * 0.0004
        val lngJitter = (Random.nextDouble() - 0.5) * 0.0004
        val lat = baseLat + latJitter
        val lng = baseLng + lngJitter
        val altitude = 22.0 + Random.nextDouble(-1.5, 1.5)
        val accuracy = 3.5f + Random.nextFloat() * 1.5f
        return GpsLocationData(
            latitude = lat,
            longitude = lng,
            altitude = altitude,
            accuracyMeters = accuracy,
            timestamp = System.currentTimeMillis(),
            isRealGps = isReal,
            provider = if (isReal) "Real GPS" else "GPS Simulated Anchor"
        )
    }
}
