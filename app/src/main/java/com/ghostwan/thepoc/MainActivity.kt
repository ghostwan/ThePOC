package com.ghostwan.thepoc

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.location.Geocoder
import java.util.Locale
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import android.provider.Settings
import androidx.core.app.ActivityCompat

private const val TAG = "MapScreen"
private const val MIN_RADIUS = 50f // 50 mètres minimum
private const val MAX_RADIUS = 500f // 500 mètres maximum
private const val GEOFENCE_RADIUS = 100f // 100 mètres par défaut
private const val GEOFENCE_EXPIRATION = 24 * 60 * 60 * 1000L // 24 heures

data class GeofenceData(
    val id: String,
    val latLng: LatLng,
    val radius: Float = GEOFENCE_RADIUS,
    val name: String
)

class MainActivity : ComponentActivity() {
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var database: AppDatabase
    lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var cameraPositionState: CameraPositionState? = null
    var isLocationTrackingEnabled = false
    
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geofencingClient = LocationServices.getGeofencingClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        database = AppDatabase.getDatabase(this)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(
                            LatLng(48.8566, 2.3522), // Centre de Paris par défaut
                            12f
                        )
                    }
                    cameraPositionState = cameraState
                    MapScreen(geofencingClient, geofencePendingIntent, database, cameraState)
                }
            }
        }
        
        setupLocationUpdates()
        
        // Centrer sur la position actuelle au démarrage
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    lifecycleScope.launch {
                        cameraPositionState?.animate(
                            update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                            durationMs = 1000
                        )
                    }
                }
            }
        }
        
        try {
            val status = MapsInitializer.initialize(applicationContext)
            Log.d(TAG, "Maps initialized with status: $status")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Maps: ${e.message}", e)
        }
    }

    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .setMaxUpdateDelayMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude}")
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                mainLooper
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    fun centerOnCurrentLocation(cameraState: CameraPositionState) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    lifecycleScope.launch {
                        try {
                            if (!cameraState.isMoving) {
                                cameraState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                                    durationMs = 1000
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error animating camera: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    geofencingClient: GeofencingClient,
    geofencePendingIntent: PendingIntent,
    database: AppDatabase,
    cameraPositionState: CameraPositionState
) {
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val paris = LatLng(48.8566, 2.3522)

    var isMapLoaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAddingGeofence by remember { mutableStateOf(false) }
    var geofences by remember { mutableStateOf(listOf<GeofenceData>()) }
    var showNameDialog by remember { mutableStateOf<LatLng?>(null) }
    var selectedGeofence by remember { mutableStateOf<GeofenceData?>(null) }
    var isEditingRadius by remember { mutableStateOf(false) }
    var editingRadius by remember { mutableStateOf(GEOFENCE_RADIUS) }
    var showDeleteConfirmation by remember { mutableStateOf<GeofenceData?>(null) }
    var showRenameDialog by remember { mutableStateOf<GeofenceData?>(null) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var zoneName by remember { mutableStateOf("") }
    var isLocationTrackingEnabled by remember { mutableStateOf(false) }
    var movingGeofence by remember { mutableStateOf<GeofenceData?>(null) }
    var markerPressStartTime by remember { mutableStateOf<Long?>(null) }
    var pressedGeofence by remember { mutableStateOf<GeofenceData?>(null) }

    var hasLocationPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var shouldShowBackgroundPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasFineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true
        } else {
            true
        }
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true
        }
        
        hasLocationPermission = hasFineLocation && hasBackgroundLocation && hasNotificationPermission
        
        if (!hasFineLocation) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                showPermissionDialog = true
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocation) {
            shouldShowBackgroundPermissionDialog = true
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasLocationPermission = true
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            ) {
                showPermissionDialog = true
            }
        }
    }

    fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun checkLocationPermissions() {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED)

        if (!hasLocationPermission) {
            requestLocationPermissions()
        }
    }

    LaunchedEffect(Unit) {
        checkLocationPermissions()
    }

    // Charger les geofences depuis la base de données
    LaunchedEffect(Unit) {
        database.geofenceDao().getAllGeofences().collectLatest { entities ->
            geofences = entities.map { entity ->
                GeofenceData(
                    id = entity.id.toString(),
                    latLng = entity.toLatLng(),
                    radius = entity.radius,
                    name = entity.name
                )
            }
        }
    }

    // Fonction pour obtenir l'adresse à partir des coordonnées
    fun getAddressFromLocation(latLng: LatLng, onAddressReceived: (String) -> Unit) {
        isLoadingAddress = true
        val geocoder = Geocoder(context, Locale.getDefault())
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                    val address = addresses.firstOrNull()?.let { addr ->
                        when {
                            addr.thoroughfare != null -> addr.thoroughfare
                            addr.subLocality != null -> addr.subLocality
                            addr.locality != null -> addr.locality
                            else -> "%.2f, %.2f".format(latLng.latitude, latLng.longitude)
                        }
                    } ?: "%.2f, %.2f".format(latLng.latitude, latLng.longitude)
                    
                    onAddressReceived(address)
                    isLoadingAddress = false
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val address = addresses?.firstOrNull()?.let { addr ->
                    when {
                        addr.thoroughfare != null -> addr.thoroughfare
                        addr.subLocality != null -> addr.subLocality
                        addr.locality != null -> addr.locality
                        else -> "%.2f, %.2f".format(latLng.latitude, latLng.longitude)
                    }
                } ?: "%.2f, %.2f".format(latLng.latitude, latLng.longitude)
                
                onAddressReceived(address)
                isLoadingAddress = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting address", e)
            onAddressReceived("%.2f, %.2f".format(latLng.latitude, latLng.longitude))
            isLoadingAddress = false
        }
    }

    // Fonction pour ajouter une geofence
    fun addGeofence(latLng: LatLng, name: String) {
        val geofence = Geofence.Builder()
            .setRequestId(name)
            .setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS)
            .setExpirationDuration(GEOFENCE_EXPIRATION)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    scope.launch {
                        // Sauvegarder dans la base de données
                        val entity = GeofenceEntity.fromLatLng(name, latLng)
                        val id = database.geofenceDao().insertGeofence(entity)
                        
                        // Mettre à jour l'interface
                        geofences = geofences + GeofenceData(
                            id = id.toString(),
                            latLng = latLng,
                            name = name
                        )
                        
                        Toast.makeText(
                            context,
                            context.getString(R.string.zone_added_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding geofence", e)
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_adding_zone, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        isAddingGeofence = false
    }

    // Fonction pour mettre à jour une geofence
    fun updateGeofenceRadius(geofence: GeofenceData, newRadius: Float) {
        scope.launch {
            val entity = GeofenceEntity(
                id = geofence.id.toLong(),
                name = geofence.name,
                latitude = geofence.latLng.latitude,
                longitude = geofence.latLng.longitude,
                radius = newRadius
            )
            database.geofenceDao().updateGeofence(entity)
            
            // Mettre à jour l'interface
            geofences = geofences.map { 
                if (it.id == geofence.id) it.copy(radius = newRadius)
                else it
            }
            
            Toast.makeText(
                context,
                context.getString(R.string.zone_updated),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Fonction pour supprimer une geofence
    fun deleteGeofence(geofence: GeofenceData) {
        scope.launch {
            val entity = GeofenceEntity(
                id = geofence.id.toLong(),
                name = geofence.name,
                latitude = geofence.latLng.latitude,
                longitude = geofence.latLng.longitude,
                radius = geofence.radius
            )
            
            // Supprimer de la base de données
            database.geofenceDao().deleteGeofence(entity)
            
            // Supprimer du système de geofencing
            geofencingClient.removeGeofences(listOf(geofence.name))
            
            // Mettre à jour l'interface
            geofences = geofences.filter { it.id != geofence.id }
            selectedGeofence = null
            
            Toast.makeText(
                context,
                context.getString(R.string.zone_deleted),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Fonction pour renommer une geofence
    fun renameGeofence(geofence: GeofenceData, newName: String) {
        scope.launch {
            // Supprimer l'ancienne geofence du système
            geofencingClient.removeGeofences(listOf(geofence.name))
            
            // Créer une nouvelle geofence avec le nouveau nom
            val newGeofence = Geofence.Builder()
                .setRequestId(newName)
                .setCircularRegion(
                    geofence.latLng.latitude,
                    geofence.latLng.longitude,
                    geofence.radius
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(newGeofence)
                .build()

            // Mettre à jour la base de données
            val entity = GeofenceEntity(
                id = geofence.id.toLong(),
                name = newName,
                latitude = geofence.latLng.latitude,
                longitude = geofence.latLng.longitude,
                radius = geofence.radius
            )
            database.geofenceDao().updateGeofence(entity)
            
            // Ajouter la nouvelle geofence au système
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener {
                        // Mettre à jour l'interface
                        geofences = geofences.map { 
                            if (it.id == geofence.id) it.copy(name = newName)
                            else it
                        }
                        selectedGeofence = selectedGeofence?.copy(name = newName)
                        
                        Toast.makeText(
                            context,
                            context.getString(R.string.zone_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating geofence", e)
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_adding_zone, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    // Fonction pour déplacer une geofence
    fun moveGeofence(geofence: GeofenceData, newPosition: LatLng) {
        scope.launch {
            // Supprimer l'ancienne geofence du système
            geofencingClient.removeGeofences(listOf(geofence.name))
            
            // Créer une nouvelle geofence avec la nouvelle position
            val newGeofence = Geofence.Builder()
                .setRequestId(geofence.name)
                .setCircularRegion(
                    newPosition.latitude,
                    newPosition.longitude,
                    geofence.radius
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(newGeofence)
                .build()

            // Mettre à jour la base de données
            val entity = GeofenceEntity(
                id = geofence.id.toLong(),
                name = geofence.name,
                latitude = newPosition.latitude,
                longitude = newPosition.longitude,
                radius = geofence.radius
            )
            database.geofenceDao().updateGeofence(entity)
            
            // Ajouter la nouvelle geofence au système
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                    .addOnSuccessListener {
                        // Mettre à jour l'interface
                        geofences = geofences.map { 
                            if (it.id == geofence.id) it.copy(latLng = newPosition)
                            else it
                        }
                        
                        Toast.makeText(
                            context,
                            context.getString(R.string.zone_moved),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating geofence", e)
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_moving_zone, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    // Désactiver le suivi de position quand l'utilisateur déplace la carte manuellement
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            isLocationTrackingEnabled = false
            if (context is MainActivity) {
                context.isLocationTrackingEnabled = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Place, contentDescription = stringResource(R.string.tab_map)) },
                    label = { Text(stringResource(R.string.tab_map)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = stringResource(R.string.tab_zones)) },
                    label = { Text(stringResource(R.string.tab_zones)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.tab_timeline)) },
                    label = { Text(stringResource(R.string.tab_timeline)) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    // Contenu de la carte
                    Box(modifier = Modifier.fillMaxSize()) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(
                                myLocationButtonEnabled = false,
                                rotationGesturesEnabled = true,
                                tiltGesturesEnabled = true,
                                zoomControlsEnabled = false,
                                zoomGesturesEnabled = true
                            ),
                            properties = MapProperties(
                                isMyLocationEnabled = hasLocationPermission
                            ),
                            onMapLoaded = {
                                isMapLoaded = true
                            },
                            onMapClick = { latLng ->
                                if (isAddingGeofence) {
                                    getAddressFromLocation(latLng) { address ->
                                        showNameDialog = latLng
                                        zoneName = address
                                    }
                                } else if (movingGeofence != null) {
                                    moveGeofence(movingGeofence!!, latLng)
                                    movingGeofence = null
                                    editingRadius = GEOFENCE_RADIUS
                                } else {
                                    selectedGeofence = null
                                    isEditingRadius = false
                                    editingRadius = GEOFENCE_RADIUS
                                }
                            }
                        ) {
                            // Afficher les zones existantes
                            geofences.forEach { geofence ->
        Marker(
                                    state = MarkerState(position = geofence.latLng),
                                    title = geofence.name,
                                    snippet = context.getString(R.string.geofencing_zone),
                                    onClick = {
                                        if (movingGeofence == null) {
                                            selectedGeofence = geofence
                                            editingRadius = geofence.radius
                                        }
                                        true
                                    }
                                )
                                Circle(
                                    center = geofence.latLng,
                                    radius = geofence.radius.toDouble(),
                                    strokeWidth = 2f,
                                    strokeColor = when {
                                        geofence == movingGeofence -> Color.Yellow
                                        geofence == selectedGeofence -> Color.Red
                                        else -> Color.Blue
                                    },
                                    fillColor = when {
                                        geofence == movingGeofence -> Color.Yellow.copy(alpha = 0.3f)
                                        geofence == selectedGeofence -> Color.Red.copy(alpha = 0.3f)
                                        else -> Color.Blue.copy(alpha = 0.3f)
                                    }
                                )
                                
                                if (geofence == selectedGeofence && isEditingRadius) {
                                    Circle(
                                        center = geofence.latLng,
                                        radius = editingRadius.toDouble(),
                                        strokeWidth = 3f,
                                        strokeColor = Color.Green,
                                        fillColor = Color.Transparent
                                    )
                                }
                            }
                        }

                        // Boutons flottants
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .padding(bottom = 72.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    if (context is MainActivity) {
                                        context.centerOnCurrentLocation(cameraPositionState)
                                    }
                                },
                                containerColor = if (isLocationTrackingEnabled) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            ) {
                                Icon(
                                    imageVector = if (isLocationTrackingEnabled) 
                                        Icons.Default.MyLocation 
                                    else 
                                        Icons.Default.LocationOn,
                                    contentDescription = stringResource(R.string.my_location)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            FloatingActionButton(
                                onClick = { isAddingGeofence = !isAddingGeofence },
                                containerColor = if (isAddingGeofence) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_zone)
                                )
                            }
                        }
                    }

                    // Loading indicator
                    if (!isMapLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // Zone details card
                    AnimatedVisibility(
                        visible = selectedGeofence != null,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    ) {
                        selectedGeofence?.let { geofence ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = context.getString(R.string.zone_details),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        IconButton(onClick = { selectedGeofence = null }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = context.getString(R.string.close)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = geofence.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Text(
                                        text = context.getString(R.string.zone_radius, editingRadius.toInt()),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    if (isEditingRadius) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = context.getString(R.string.drag_to_resize),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Slider(
                                            value = editingRadius,
                                            onValueChange = { editingRadius = it },
                                            valueRange = MIN_RADIUS..MAX_RADIUS
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = context.getString(R.string.min_radius),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = context.getString(R.string.max_radius),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                updateGeofenceRadius(geofence, editingRadius)
                                                isEditingRadius = false
                                                selectedGeofence = selectedGeofence?.copy(radius = editingRadius)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(context.getString(R.string.save))
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = context.getString(R.string.zone_actions),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { showRenameDialog = geofence },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(context.getString(R.string.edit_zone_name))
                                            }
                                            
                                            OutlinedButton(
                                                onClick = { isEditingRadius = true },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(context.getString(R.string.edit_zone_radius))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { 
                                                    movingGeofence = geofence
                                                    selectedGeofence = null
                                                    isEditingRadius = false
                                                    editingRadius = GEOFENCE_RADIUS
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.tap_to_move_zone),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PanTool,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(context.getString(R.string.move_zone))
                                            }
                                            Button(
                                                onClick = { showDeleteConfirmation = geofence },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(context.getString(R.string.delete_zone))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Liste des zones
                    ZonesList(
                        zones = geofences,
                        onZoneClick = { zone ->
                            selectedGeofence = zone
                            selectedTab = 0 // Retour à la carte
                            scope.launch {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(zone.latLng, 15f),
                                    durationMs = 1000
                                )
                            }
                        },
                        onEditClick = { zone ->
                            showRenameDialog = zone
                        },
                        onDeleteClick = { zone ->
                            showDeleteConfirmation = zone
                        }
                    )
                }
                2 -> {
                    // Timeline
                    TimelineScreen(database)
                }
            }
        }
    }

    // Dialogues
    if (showNameDialog != null) {
        ZoneNameDialog(
            isLoading = isLoadingAddress,
            initialValue = zoneName,
            onDismiss = { showNameDialog = null },
            onConfirm = { name ->
                showNameDialog?.let { latLng ->
                    addGeofence(latLng, name)
                }
                showNameDialog = null
            }
        )
    }

    if (showDeleteConfirmation != null) {
        DeleteConfirmationDialog(
            zoneName = showDeleteConfirmation!!.name,
            onConfirm = {
                deleteGeofence(showDeleteConfirmation!!)
                showDeleteConfirmation = null
            },
            onDismiss = { showDeleteConfirmation = null }
        )
    }

    if (showRenameDialog != null) {
        RenameZoneDialog(
            currentName = showRenameDialog!!.name,
            onConfirm = { newName ->
                renameGeofence(showRenameDialog!!, newName)
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(context.getString(R.string.location_permission_required)) },
            text = { Text(context.getString(R.string.location_permission_explanation)) },
            confirmButton = {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        })
                        showPermissionDialog = false
                    }
                ) {
                    Text(context.getString(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }

    if (shouldShowBackgroundPermissionDialog) {
        AlertDialog(
            onDismissRequest = { shouldShowBackgroundPermissionDialog = false },
            title = { Text(context.getString(R.string.location_permission_required)) },
            text = { Text(context.getString(R.string.background_location_permission_explanation)) },
            confirmButton = {
                Button(onClick = {
                    requestBackgroundLocationPermission()
                    shouldShowBackgroundPermissionDialog = false
                }) {
                    Text(context.getString(R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { shouldShowBackgroundPermissionDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ZonesList(
    zones: List<GeofenceData>,
    onZoneClick: (GeofenceData) -> Unit,
    onEditClick: (GeofenceData) -> Unit,
    onDeleteClick: (GeofenceData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.zones_list),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        
        if (zones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_zones),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(zones) { zone ->
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = zone.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.zone_radius, zone.radius.toInt()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onEditClick(zone) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.edit_zone_name)
                                    )
                                }
                                IconButton(onClick = { onDeleteClick(zone) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_zone)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onZoneClick(zone) }
                    )
                    if (zones.last() != zone) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneNameDialog(
    isLoading: Boolean,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var zoneName by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.name_zone)) },
        text = {
            Column {
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text(stringResource(R.string.zone_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (zoneName.isNotBlank()) {
                        onConfirm(zoneName)
                    }
                },
                enabled = zoneName.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    zoneName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_delete_title)) },
        text = { 
            Text(stringResource(R.string.confirm_delete_message, zoneName))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete_zone))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun RenameZoneDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_zone)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.new_zone_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName)
                    }
                },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
} 