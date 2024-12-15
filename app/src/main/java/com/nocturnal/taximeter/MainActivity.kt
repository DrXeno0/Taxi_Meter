package com.nocturnal.taximeter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import android.graphics.Color.BLACK
import android.graphics.Color.TRANSPARENT
import android.graphics.Color.WHITE
import android.graphics.Color.YELLOW
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.nocturnal.taximeter.utils.NotificationHelper
import com.nocturnal.taximeter.utils.PermissionsHelper
import com.nocturnal.taximeter.viewmodel.MainViewModel
import com.nocturnal.taximeter.viewmodel.MainViewModelFactory
import kotlinx.coroutines.delay
import pub.devrel.easypermissions.EasyPermissions

import androidx.compose.material.*
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.nocturnal.taximeter.data.Ride
import com.nocturnal.taximeter.data.TaxiDriver
import com.nocturnal.taximeter.utils.PermissionsHelper.LOCATION_PERMISSION_CODE
import com.nocturnal.taximeter.viewmodel.RideViewModel
import com.nocturnal.taximeter.viewmodel.TaxiDriverViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var notificationHelper: NotificationHelper
    private var isRideActive: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 5000
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var taxiDriverViewModel: TaxiDriverViewModel
    private lateinit var rideViewModel: RideViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = MainViewModelFactory(this)
        mainViewModel = ViewModelProvider(this, factory).get(MainViewModel::class.java)
        notificationHelper = NotificationHelper(this)
        taxiDriverViewModel = ViewModelProvider(this).get(TaxiDriverViewModel::class.java)
        rideViewModel = ViewModelProvider(this).get(RideViewModel::class.java)


        setContent {


            val driver = taxiDriverViewModel.driver.observeAsState()


            taxiDriverViewModel.loadDriver()


            if (driver.value == null) {
                ProfileInputScreen(taxiDriverViewModel)
            } else {
                AppNavigator(mainViewModel, notificationHelper, rideViewModel)
            }


        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        PermissionsHelper.checkAndPromptLocationServices(
            this,
            onLocationSettingsSatisfied = {
                Toast.makeText(this, "Location services are already enabled.", Toast.LENGTH_SHORT)
                    .show()
            },
            onResolutionRequired = { exception ->
                try {
                    exception.startResolutionForResult(
                        this,
                        PermissionsHelper.LOCATION_REQUEST_CODE
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(
                        this,
                        "Unable to start resolution: ${sendEx.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onFailure = { exception ->
                Toast.makeText(
                    this,
                    "Failed to check location settings: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

    }


    private val updateRideTask = object : Runnable {
        override fun run() {
            if (isRideActive) {
                mainViewModel.updateLocation()
                handler.postDelayed(this, updateInterval)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (EasyPermissions.somePermissionPermanentlyDenied(this, permissions.toList())) {
            PermissionsHelper.showSettingsDialog(this)
        } else if (PermissionsHelper.hasLocationPermission(this)) {
            Toast.makeText(
                this,
                "Permission granted. You can now start the ride.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, "Permission denied. Cannot start the ride.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsHelper.LOCATION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Location services enabled!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location services not enabled.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @Composable
    fun AppNavigator(viewModel: MainViewModel, notificationHelper: NotificationHelper, rideViewModel: RideViewModel) {
        val navController = rememberNavController()
        val items = listOf(
            BottomNavItem("main_screen", "Home", Icons.Default.Home),
            BottomNavItem("profile_page", "Profile", Icons.Default.Person),
            BottomNavItem("history_page", "History", Icons.Default.DateRange)
        )

        Scaffold(

            bottomBar = {
                BottomNavigationBar(navController = navController, items = items)
            },

            ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .background(
                        brush = Brush.verticalGradient(
                            0.1f to Color(0xff343A40),
                            1f to Color(0xff1e1e1e)
                        )
                    )
            ) {
                androidx.navigation.compose.NavHost(
                    navController = navController,
                    startDestination = "main_screen"

                ) {
                    composable("main_screen") {
                        MainScreen(navController, viewModel, notificationHelper,  rideViewModel )
                    }
                    composable("profile_page") {
                        val driver = taxiDriverViewModel.driver.observeAsState().value
                        if (driver == null) {

                            ProfileInputScreen(taxiDriverViewModel)
                        } else {

                            ProfilePage(driver)
                        }
                    }
                    composable("History_page") {
                        HistoryScreen(rideViewModel)
                    }
                }
            }
        }
    }

    @Composable
    fun MainScreen(
        navController: NavController,
        viewModel: MainViewModel,
        notificationHelper: NotificationHelper,
        rideViewModel: RideViewModel
    ) {

        val context = LocalContext.current
        var isRideActive by remember { mutableStateOf(false) }
        val rideData by viewModel.rideData.observeAsState()
        val hasLocationPermission = remember {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        val mapProperties = MapProperties(
            true,

            isMyLocationEnabled = true,
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style),
            isTrafficEnabled = true,
            isIndoorEnabled = true,

            )

        LaunchedEffect(isRideActive) {
            while (isRideActive) {
                viewModel.updateLocation()
                delay(5000)
            }
        }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 15f)
        }

        LaunchedEffect(Unit) {


            try {
                val locationClient = LocationServices.getFusedLocationProviderClient(context)
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    EasyPermissions.requestPermissions(
                        this@MainActivity,
                        "This app needs access to your location to track rides.",
                        LOCATION_PERMISSION_CODE,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                } else {
                    locationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(location.latitude, location.longitude),
                                15f
                            )
                        }
                    }

                }
            } catch (e: Exception) {
                Log.e("MainScreen", "Error getting permission $e")
            }


        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.1f to Color(0xff343A40),
                        1f to Color(0xff1e1e1e)
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(23.dp)
            ) {

                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.Center) {

                    if (hasLocationPermission) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties
                    ) }else{
                        PermissionsHelper.requestLocationPermission(this@MainActivity)
                        Log.d("MainScreen", "Permission not granted")


                    }


                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.align(Alignment.CenterHorizontally)) {
                        InformationComposable(
                            "${
                                rideData?.distance?.let {
                                    String.format(
                                        "%.2f km",
                                        it
                                    )
                                } ?: "N/A"
                            }")
                        Spacer(Modifier.height(10.dp))
                        InformationComposable("${rideData?.timeElapsed?.let { "$it min" } ?: "N/A"}")
                        Spacer(Modifier.height(10.dp))
                        InformationComposable(
                            "${
                                rideData?.totalFare?.let {
                                    String.format(
                                        "%.2f DH",
                                        it
                                    )
                                } ?: "N/A"
                            }")
                    }
                    Spacer(Modifier.height(15.dp))
                    Button(
                        onClick = {
                            if (!isRideActive) {
                                if (!PermissionsHelper.hasLocationPermission(context)) {
                                    PermissionsHelper.requestLocationPermission(context as Activity)
                                    Log.d("MainScreen", "Permission not granted")
                                    return@Button

                                }
                                isRideActive = true
                                viewModel.startRide()
                                handler.postDelayed(updateRideTask, updateInterval)
                            } else {
                                isRideActive = false
                                viewModel.endRide()
                                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                    Date()
                                )
                                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                val duration = "${rideData?.timeElapsed} mins"  // Calculate ride duration
                                val fee = rideData?.totalFare ?: 0.0

                                rideViewModel.saveRide(currentDate, currentTime, duration, fee)
                                rideData?.let { ride ->
                                    notificationHelper.sendFareNotification(
                                        ride.totalFare, ride.distance, ride.timeElapsed
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(1.dp)
                            .width(271.dp)
                            .height(64.dp)
                            .align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xffFFD700)
                        ), shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isRideActive) "End Ride" else "Start Ride",
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }


    @Composable
    fun InformationComposable(text: String = "Information") {
        Box(
            modifier = Modifier
                .padding(0.dp)
                .width(255.dp)
                .height(64.dp)
                .background(color = Color(0xff1e1e1e), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontFamily = FontFamily(Font(R.font.lexend_peta)),
                    fontWeight = FontWeight(600),
                    color = Color(0xFFFFFFFF)
                )
            )
        }
    }


    @Composable
    fun ProfilePage(driver: TaxiDriver) {
        val qrContent = driver.qrCodeContent
        val qrBitmap = generateQrCode(driver.qrCodeContent)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(26.dp)

            ) {
                Text(
                    "Hi ${driver.fullName} !",
                    style = MaterialTheme.typography.h2,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(25.dp))
                Text(
                    "your car is ${driver.car}",
                    style = MaterialTheme.typography.h6,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    "License Type: ${driver.licenseType}",
                    style = MaterialTheme.typography.h6,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(50.dp))





            if (qrContent.isNotEmpty()) {


                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp)
                )
            } else {

                Text(text = "QR Code content is empty", color = Color.Red)
            }

        }

    }

    fun generateQrCode(content: String): Bitmap {
        if (content.isEmpty()) {
            throw IllegalArgumentException("QR Code content cannot be empty")
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap.setPixel(x, y, (if (bitMatrix[x, y]) BLACK else WHITE))
            }
        }
        return bitmap
    }


    data class BottomNavItem(
        val route: String,
        val title: String,
        val icon: ImageVector
    )

    @Composable
    fun BottomNavigationBar(
        navController: NavController,
        items: List<BottomNavItem>
    ) {
        BottomNavigation(
            backgroundColor = Color(0xffFFD700),
            contentColor = Color.White
        ) {
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route

            items.forEach { item ->
                BottomNavigationItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    selectedContentColor = Color(0xffFFD700),
                    unselectedContentColor = Color.Gray
                )
            }
        }
    }

    @Composable
    fun SettingsPage() {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.1f to Color(0xff343A40),
                        1f to Color(0xff1e1e1e)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Settings Page",
                color = Color.White,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }


    }


    @Composable
    fun ProfileInputScreen(viewModel: TaxiDriverViewModel) {
        var fullName by remember { mutableStateOf<String>("") }
        var car by remember { mutableStateOf<String>("") }
        var licenseType by remember { mutableStateOf<String>("") }
        var profilePhotoUri by remember { mutableStateOf<String>("") } // Update when user selects photo

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.1f to Color(0xff343A40),
                        1f to Color(0xff1e1e1e)
                    )
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = R.drawable.taxi), contentDescription = "icon")
            OutlinedTextField(

                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") }
                ,
                colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xffFFD700),
                    unfocusedBorderColor = Color.Black,
                    textColor = Color.White,
                    focusedLabelColor = Color(0xffFFD700),
                    cursorColor = Color(0xffFFD700),
                    unfocusedLabelColor = Color.Gray
                )
                ,
                maxLines = 1,

            )
            OutlinedTextField(
                value = car,
                onValueChange = { car = it },
                label = { Text("Car") }
                ,
                colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xffFFD700),
                    unfocusedBorderColor = Color.Black,
                    textColor = Color.White,
                    focusedLabelColor = Color(0xffFFD700),
                    cursorColor = Color(0xffFFD700),
                    unfocusedLabelColor = Color.Gray
                )
                       , maxLines = 1,
            )
            OutlinedTextField(
                value = licenseType,
                onValueChange = { licenseType = it },
                label = { Text("License Type") }
                ,
                colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xffFFD700),
                    unfocusedBorderColor = Color.Black,
                    textColor = Color.White,
                    focusedLabelColor = Color(0xffFFD700),
                    cursorColor = Color(0xffFFD700),
                    unfocusedLabelColor = Color.Gray
                )
                ,maxLines = 1,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier
                    .padding(1.dp)
                    .width(271.dp)
                    .height(64.dp),
                colors = androidx.compose.material.ButtonDefaults.buttonColors(Color(0xffFFD700)),
                onClick = {
                    val qrContent = "$fullName, $car, $licenseType"
                    val driver = TaxiDriver(
                        fullName = fullName,
                        car = car,
                        licenseType = licenseType,
                        profilePhotoUri = profilePhotoUri,
                        qrCodeContent = qrContent
                    )
                    viewModel.saveDriver(driver)
                }) {
                Text("Save Profile")
            }
        }
    }

    @Composable
    fun HistoryScreen(rideViewModel: RideViewModel) {
        val rides by rideViewModel.rideHistory.observeAsState(emptyList())

        LaunchedEffect(Unit) {
            rideViewModel.fetchRideHistory()
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Ride History",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 16.dp, top = 16.dp),
                color = Color.White

            )
            LazyColumn {
                items(rides) { ride ->
                    RideHistoryItem(ride)
                }
            }
        }
    }

    @Composable
    fun RideHistoryItem(ride: Ride) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(Color(0xffFFD700), shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(text = "Date: ${ride.date}", style = MaterialTheme.typography.body1)
                Text(text = "Time: ${ride.time}", style = MaterialTheme.typography.body2)
                Text(text = "Duration: ${ride.duration}", style = MaterialTheme.typography.body2)
                Text(text = "Fee: ${String.format("%.2f DH", ride.fee)}", style = MaterialTheme.typography.body1)
            }
        }
    }



}