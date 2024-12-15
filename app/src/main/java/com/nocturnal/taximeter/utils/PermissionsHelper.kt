package com.nocturnal.taximeter.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import pub.devrel.easypermissions.EasyPermissions


object PermissionsHelper {
    const val LOCATION_PERMISSION_CODE = 100
    const val LOCATION_REQUEST_CODE = 101

    fun hasLocationPermission(context: Context): Boolean {
        return EasyPermissions.hasPermissions(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun requestLocationPermission(activity: Activity) {
        EasyPermissions.requestPermissions(
            activity,
            "This app needs access to your location to track rides.",
            LOCATION_PERMISSION_CODE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun showSettingsDialog(context: Context) {
        android.app.AlertDialog.Builder(context)
            .setTitle("Permissions Required")
            .setMessage("Location permission is necessary for the app to function. Please enable it in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    fun checkAndPromptLocationServices(
        context: Context,
        onLocationSettingsSatisfied: () -> Unit,
        onResolutionRequired: (ResolvableApiException) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingsClient = LocationServices.getSettingsClient(context)
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                onLocationSettingsSatisfied()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    onResolutionRequired(exception)
                } else {
                    onFailure(exception)
                }
            }
    }
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }

    private const val LOCATION_PERMISSION_REQUEST_CODE = 100
}
