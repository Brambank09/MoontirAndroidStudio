package com.moontir.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address as AndroidAddress
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

sealed class LocationFetchResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationFetchResult()
    data object PermissionDenied : LocationFetchResult()
    data object LocationDisabled : LocationFetchResult()
    data class Error(val reason: String) : LocationFetchResult()
}

fun hasLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

fun isDeviceLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return try {
        LocationManagerCompat.isLocationEnabled(lm)
    } catch (_: Throwable) {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

/**
 * Queries the phone's real GPS / Fused Location sensor for the user's current position.
 * Tries:
 * 1. Google Play Services FusedLocationProviderClient fresh high-accuracy fix
 * 2. Android LocationManager GPS/Network sensor fresh fix (works even without Play Services)
 * 3. Freshest cached last-known location from Fused or LocationManager
 */
@SuppressLint("MissingPermission")
suspend fun fetchCurrentDeviceLocation(context: Context): LocationFetchResult {
    if (!hasLocationPermission(context)) {
        return LocationFetchResult.PermissionDenied
    }
    if (!isDeviceLocationEnabled(context)) {
        return LocationFetchResult.LocationDisabled
    }

    // 1. Try fresh high-accuracy fix via FusedLocationProviderClient
    val fusedCurrent = withTimeoutOrNull(7000L) {
        requestFusedCurrentLocation(context)
    }
    if (fusedCurrent != null) {
        return LocationFetchResult.Success(fusedCurrent.latitude, fusedCurrent.longitude)
    }

    // 2. Try direct Android LocationManager GPS sensor (then Network provider)
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager != null) {
        val providers = buildList {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }
        for (provider in providers) {
            val directFix = withTimeoutOrNull(5500L) {
                requestLocationManagerCurrentFix(context, locationManager, provider)
            }
            if (directFix != null) {
                return LocationFetchResult.Success(directFix.latitude, directFix.longitude)
            }
        }
    }

    // 3. Fallback to most recent cached last-known location
    val candidates = mutableListOf<Location>()
    val fusedLast = withTimeoutOrNull(2500L) {
        requestFusedLastLocation(context)
    }
    if (fusedLast != null) candidates.add(fusedLast)

    if (locationManager != null) {
        listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).forEach { provider ->
            try {
                locationManager.getLastKnownLocation(provider)?.let { candidates.add(it) }
            } catch (_: Throwable) {}
        }
    }

    val best = candidates.maxByOrNull { it.time }
    if (best != null) {
        return LocationFetchResult.Success(best.latitude, best.longitude)
    }

    return LocationFetchResult.Error("No GPS fix available")
}

@SuppressLint("MissingPermission")
private suspend fun requestFusedCurrentLocation(context: Context): Location? =
    suspendCancellableCoroutine { cont ->
        val tokenSource = CancellationTokenSource()
        cont.invokeOnCancellation { tokenSource.cancel() }
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val priority = if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }
            client.getCurrentLocation(priority, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
                .addOnCanceledListener {
                    if (cont.isActive) cont.resume(null)
                }
        } catch (_: Throwable) {
            if (cont.isActive) cont.resume(null)
        }
    }

@SuppressLint("MissingPermission")
private suspend fun requestFusedLastLocation(context: Context): Location? =
    suspendCancellableCoroutine { cont ->
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation
                .addOnSuccessListener { location ->
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        } catch (_: Throwable) {
            if (cont.isActive) cont.resume(null)
        }
    }

@SuppressLint("MissingPermission")
private suspend fun requestLocationManagerCurrentFix(
    context: Context,
    locationManager: LocationManager,
    provider: String
): Location? = suspendCancellableCoroutine { cont ->
    val signal = CancellationSignal()
    cont.invokeOnCancellation { signal.cancel() }
    try {
        LocationManagerCompat.getCurrentLocation(
            locationManager,
            provider,
            signal,
            ContextCompat.getMainExecutor(context)
        ) { location ->
            if (cont.isActive) cont.resume(location)
        }
    } catch (_: Throwable) {
        if (cont.isActive) cont.resume(null)
    }
}

/**
 * Uses Android's built-in Geocoder as a fallback for reverse geocoding when Nominatim is unreachable.
 */
suspend fun androidGeocoderReverseLookup(context: Context, lat: Double, lon: Double): String? =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<AndroidAddress>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(4000L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(results: MutableList<AndroidAddress>) {
                                if (cont.isActive) cont.resume(results)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)
            }
            val first = addresses?.firstOrNull() ?: return@withContext null
            val line = first.getAddressLine(0)
            if (!line.isNullOrBlank()) {
                line
            } else {
                listOfNotNull(first.thoroughfare, first.subLocality, first.locality, first.adminArea)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }
            }
        } catch (_: Throwable) {
            null
        }
    }
