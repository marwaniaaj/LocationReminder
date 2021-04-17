package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient

    // Check what API the device is running.
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        setTitle(getString(R.string.save_reminder))

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        binding.saveReminder.setOnClickListener {

            val reminderData = ReminderDataItem(
                    title = _viewModel.reminderTitle.value,
                    description = _viewModel.reminderDescription.value,
                    location = _viewModel.reminderSelectedLocationStr.value,
                    latitude = _viewModel.latitude.value,
                    longitude = _viewModel.longitude.value
            )

//             use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db

            saveReminder(reminderData)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    private fun saveReminder(reminderData: ReminderDataItem) {

        if (_viewModel.validateAndSaveReminder(reminderData)) {
            addGeofenceForReminder(reminderData)
        }

        Toast.makeText(requireActivity(), _viewModel.showToast.value,
            Toast.LENGTH_SHORT)
            .show()

        _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToReminderListFragment())

    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder(reminderData: ReminderDataItem) {
        // Add Geofence
        reminderData.let { reminder ->

            // Build the Geofence Object
            val geofence = Geofence.Builder()
                    // Set the request ID, string to identify the geofence.
                    .setRequestId(reminder.id)
                    // Set the circular region of this geofence.
                    .setCircularRegion(reminder.latitude!!,
                            reminder.longitude!!,
                            GEOFENCE_RADIUS_IN_METERS)
                    // Set the expiration duration of the geofence.
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    // Set the transition types of interest.
                    // We track entry transitions in this App.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()

            // Build the geofence request
            val geofenceRequest = GeofencingRequest.Builder()
                    // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
                    // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
                    // is already inside that geofence.
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    // Add the geofences to be monitored by geofencing service.
                    .addGeofence(geofence)
                    .build()

            val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
            intent.action = ACTION_GEOFENCE_EVENT

            // Add the new geofence request with the new geofence
            geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent)?.run {
                addOnSuccessListener {
                    // Geofences added.
                    Toast.makeText(requireActivity(), "Geofence added",
                            Toast.LENGTH_SHORT)
                            .show()
                    Log.i("Add Geofence", geofence.requestId)
                }
                addOnFailureListener {
                    // Failed to add geofences.
                    Toast.makeText(requireActivity(), R.string.geofences_not_added,
                            Toast.LENGTH_SHORT).show()
                    if ((it.message != null)) {
                        Log.w(TAG, it.message!!)
                        Log.w(TAG, it)
                    }
                }
            }
        }
    }

    /**
     * Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {

        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION))

        val backgroundPermissionApproved =
                if (runningQOrLater) {
                    PackageManager.PERMISSION_GRANTED ==
                            ActivityCompat.checkSelfPermission(requireContext(),
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    true
                }

        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /**
     * Determines whether the app has the appropriate permissions across Android 10+ and all other
     * Android versions.
     */
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {

        // If the permissions have already been approved, return out of the method.
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Permissions that are going to be requested
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        // Result code will be different depending on if the device is running Q or later
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else ->
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")

        // Request permissions passing in the current activity, the permissions array and the result code.
        ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsArray,
                resultCode
        )
    }

    /**
     * In all cases, we need to have the location permission.
     * On Android 10+ (Q) we need to have the background permission as well.
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {

        Log.d(TAG, "onRequestPermissionResult")

        if (grantResults.isEmpty() ||
                grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
                (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                        grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)) {

            Snackbar.make(this.requireView(),
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data =  Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
        }
        else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    /**
     * Uses the Location Client to check the current state of location settings,
     * and gives the user the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(requireActivity(),
                            REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                        requireView(),
                        R.string.location_required_error,
                        Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                Log.i("CheckDeviceLocation", "Granted")
            }
        }
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */
    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    override fun onStart() {
        super.onStart()
        checkPermissionsAndStartGeofencing()
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        private val TAG = SaveReminderFragment::class.java.simpleName

        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1

        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

        private const val GEOFENCE_RADIUS_IN_METERS = 500f
    }
}
