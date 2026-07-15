package com.example.ui

import android.Manifest
import com.example.utils.parseDoubleSafe
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MockLocationEntity
import com.example.utils.CountryInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LocationViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe StateFlows from ViewModel
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val customLatitude by viewModel.customLatitude.collectAsStateWithLifecycle()
    val customLongitude by viewModel.customLongitude.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val filteredCountries by viewModel.filteredCountries.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    val isMockingRunning by viewModel.isMockingRunning.collectAsStateWithLifecycle()
    val currentSpoofedCountry by viewModel.currentSpoofedCountry.collectAsStateWithLifecycle()
    val currentSpoofedLat by viewModel.currentSpoofedLat.collectAsStateWithLifecycle()
    val currentSpoofedLng by viewModel.currentSpoofedLng.collectAsStateWithLifecycle()
    val permissionException by viewModel.permissionException.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Countries, 1: Saved/History, 2: Setup Guide

    // Android Permission handling
    val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            viewModel.startSpoofing()
        } else {
            Toast.makeText(
                context,
                "يجب إعطاء صلاحيات الموقع لتشغيل التطبيق | GPS Permissions required",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val handleStartStopAction = {
        if (isMockingRunning) {
            viewModel.stopSpoofing()
            Toast.makeText(context, "تم إيقاف محاكاة الموقع | Location Spoofing Stopped", Toast.LENGTH_SHORT).show()
        } else {
            // Check custom lat/lng are populated
            val lat = parseDoubleSafe(customLatitude)
            val lng = parseDoubleSafe(customLongitude)
            if (lat == null || lng == null) {
                Toast.makeText(
                    context,
                    "يرجى تحديد دولة أو إدخال إحداثيات صحيحة | Please enter valid coordinates",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Check permissions first
                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                
                if (hasFine || hasCoarse) {
                    viewModel.startSpoofing()
                } else {
                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                }
            }
        }
    }

    // Modern cyber dark gradient theme
    val darkBgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Deep Slate Navy
            Color(0xFF020617)  // Charcoal Black
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8), // Electric Cyan
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GPS SPOOFER",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBgGradient)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // 1. ACTIVE STATE HEADER CARD (Glowing Visual Core)
                ActiveStatusCard(
                    isRunning = isMockingRunning,
                    countryName = currentSpoofedCountry,
                    latitude = currentSpoofedLat,
                    longitude = currentSpoofedLng,
                    onStopClick = { viewModel.stopSpoofing() }
                )

                // Developer Option Alert (If app is not mock-location enabled in dev options)
                if (permissionException) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, Color(0xFFF87171), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "تحذير",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "تنبيه هام | Developer Settings Required",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "يجب تحديد التطبيق كمحاكي للموقع في خيارات المطور بهاتفك. تفضل بزيارة تبويب 'دليل التشغيل' للمساعدة.",
                                    color = Color(0xFFFECACA),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. NAV TABS
                TabSelector(
                    selectedTab = activeTab,
                    onTabSelected = { activeTab = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. TAB SCENE SWITCHER
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        0 -> CountriesTab(
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            filteredCountries = filteredCountries,
                            selectedCountry = selectedCountry,
                            onCountrySelect = { viewModel.selectCountry(it) },
                            customLatitude = customLatitude,
                            onLatChange = { viewModel.onCustomLatitudeChange(it) },
                            customLongitude = customLongitude,
                            onLngChange = { viewModel.onCustomLongitudeChange(it) }
                        )
                        1 -> HistoryFavoritesTab(
                            allHistory = allHistory,
                            favorites = favorites,
                            onItemClick = { viewModel.startSpoofingEntity(it) },
                            onItemToggleFavorite = { viewModel.toggleFavorite(it) },
                            onItemDelete = { viewModel.deleteHistoryItem(it) },
                            onClearHistory = { viewModel.clearHistory() }
                        )
                        2 -> DeveloperGuideTab()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. ACTION SPOOFING BUTTON (Sticky Footer Action)
                Button(
                    onClick = {
                        keyboardController?.hide()
                        handleStartStopAction()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("action_spoof_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMockingRunning) Color(0xFFEF4444) else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isMockingRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMockingRunning) {
                                "إيقاف محاكاة الموقع | STOP SPOOFING"
                            } else {
                                "بدء محاكاة الموقع | START SPOOFING"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.navigationBarsPadding())
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// ==========================================
// COMPOSABLE COMPONENT 1: ACTIVE STATUS CARD (Highly Polished)
// ==========================================
@Composable
fun ActiveStatusCard(
    isRunning: Boolean,
    countryName: String?,
    latitude: Double?,
    longitude: Double?,
    onStopClick: () -> Unit
) {
    val radarTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by radarTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) Color(0xFF0B1E2E) else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isRunning) {
                        listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                    } else {
                        listOf(Color(0xFF475569), Color(0xFF334155))
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsating Green Status Dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isRunning) Color(0xFF10B981) else Color(0xFF94A3B8),
                                shape = CircleShape
                            )
                    ) {
                        if (isRunning) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, Color(0xFF10B981).copy(alpha = pulseAlpha), CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "الموقع نشط | SPOOF ACTIVE" else "التشغيل معطل | DISCONNECTED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) Color(0xFF34D399) else Color(0xFF94A3B8)
                    )
                }

                if (isRunning) {
                    IconButton(
                        onClick = onStopClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Radar Canvas Visuals
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF020617), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RadarVisualizer(isRunning = isRunning)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = countryName ?: "لم يتم تحديد موقع | No Location Active",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (latitude != null && longitude != null) {
                            String.format("خط عرض: %.5f\nخط طول: %.5f", latitude, longitude)
                        } else {
                            "أختر دولة لبدء محاكاة الموقع والفتح"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}

// Custom radar visual drawing animation
@Composable
fun RadarVisualizer(isRunning: Boolean) {
    val radarTransition = rememberInfiniteTransition(label = "radar")
    val scale1 by radarTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val alpha1 by radarTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val scale2 by radarTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )
    val alpha2 by radarTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    val sweepAngle by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = size / 2.0f
        val maxRadius = size.minDimension / 2.0f

        // Draw background radar concentric circles
        drawCircle(
            color = Color(0xFF1E293B),
            radius = maxRadius * 0.8f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF1E293B),
            radius = maxRadius * 0.5f,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw cross lines
        drawLine(
            color = Color(0xFF1E293B),
            start = androidx.compose.ui.geometry.Offset(0f, center.height),
            end = androidx.compose.ui.geometry.Offset(size.width, center.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFF1E293B),
            start = androidx.compose.ui.geometry.Offset(center.width, 0f),
            end = androidx.compose.ui.geometry.Offset(center.width, size.height),
            strokeWidth = 1.dp.toPx()
        )

        if (isRunning) {
            // Draw radar wave pulse 1
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = alpha1),
                radius = maxRadius * scale1,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw radar wave pulse 2
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = alpha2),
                radius = maxRadius * scale2,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw spinning sonar line
            drawArc(
                color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                startAngle = sweepAngle,
                sweepAngle = 45f,
                useCenter = true
            )

            // Central active target point
            drawCircle(
                color = Color(0xFF06B6D4),
                radius = 5.dp.toPx()
            )
        } else {
            // Static off target point
            drawCircle(
                color = Color(0xFF64748B),
                radius = 4.dp.toPx()
            )
        }
    }
}

// ==========================================
// COMPOSABLE COMPONENT 2: TAB SELECTOR
// ==========================================
@Composable
fun TabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabLabels = listOf(
        "الدول والمواقع" to "Countries",
        "المواقع المحفوظة" to "Saved Logs",
        "دليل التشغيل" to "How to Use"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabLabels.forEachIndexed { index, pair ->
            val isActive = selectedTab == index
            val animatedBg by animateColorAsState(
                targetValue = if (isActive) Color(0xFF0F172A) else Color.Transparent,
                animationSpec = tween(250),
                label = "tabBg"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(animatedBg)
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pair.first,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                    )
                    Text(
                        text = pair.second,
                        fontSize = 9.sp,
                        color = if (isActive) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 1 SCREEN: COUNTRIES & SEARCH & COORDINATES
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesTab(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    filteredCountries: List<CountryInfo>,
    selectedCountry: CountryInfo?,
    onCountrySelect: (CountryInfo) -> Unit,
    customLatitude: String,
    onLatChange: (String) -> Unit,
    customLongitude: String,
    onLngChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Manual Coordinates Inputs Expandable Section
        var showManualInputs by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showManualInputs = !showManualInputs }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إدخال إحداثيات مخصصة | Custom GPS Coordinates",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
            Icon(
                imageVector = if (showManualInputs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF38BDF8)
            )
        }

        AnimatedVisibility(
            visible = showManualInputs,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customLatitude,
                        onValueChange = onLatChange,
                        label = { Text("خط عرض (Latitude)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                    OutlinedTextField(
                        value = customLongitude,
                        onValueChange = onLngChange,
                        label = { Text("خط طول (Longitude)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "* أدخل قيم عشرية دقيقة مثل: 40.7128، -74.0060",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            placeholder = { Text("ابحث عن الدولة... (ألمانيا، مصر، إلخ)") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Countries list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCountries) { country ->
                val isThisSelected = selectedCountry?.code == country.code
                val borderBrush = if (isThisSelected) {
                    Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF38BDF8)))
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isThisSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderBrush, RoundedCornerShape(12.dp))
                        .clickable { onCountrySelect(country) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Circular Country Badge
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF1E293B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = country.code,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = country.nameAr,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = country.nameEn,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Coordinates info tag
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("Lat: %.2f", country.latitude),
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = String.format("Lng: %.2f", country.longitude),
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2 SCREEN: HISTORY & FAVORITES DATA
// ==========================================
@Composable
fun HistoryFavoritesTab(
    allHistory: List<MockLocationEntity>,
    favorites: List<MockLocationEntity>,
    onItemClick: (MockLocationEntity) -> Unit,
    onItemToggleFavorite: (MockLocationEntity) -> Unit,
    onItemDelete: (MockLocationEntity) -> Unit,
    onClearHistory: () -> Unit
) {
    if (allHistory.isEmpty() && favorites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "سجل المواقع فارغ | No History Yet",
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "عندما تبدأ بتغيير موقعك ستظهر المواقع السابقة هنا للوصول السريع.",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FAVORITES SECTION
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        text = "المواقع المفضلة ★ | Favorite Locations",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(favorites) { favorite ->
                    MockLocationRow(
                        entity = favorite,
                        onItemClick = { onItemClick(favorite) },
                        onToggleFavorite = { onItemToggleFavorite(favorite) },
                        onDelete = { onItemDelete(favorite) }
                    )
                }
            }

            // HISTORIC LIST
            if (allHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "السجل والعمليات الأخيرة ⟲ | Spoof History",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp
                        )

                        Text(
                            text = "مسح الكل | Clear All",
                            color = Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onClearHistory() }
                                .padding(4.dp)
                        )
                    }
                }

                items(allHistory) { historyItem ->
                    MockLocationRow(
                        entity = historyItem,
                        onItemClick = { onItemClick(historyItem) },
                        onToggleFavorite = { onItemToggleFavorite(historyItem) },
                        onDelete = { onItemDelete(historyItem) }
                    )
                }
            }
        }
    }
}

// Row renderer for saved item logs
@Composable
fun MockLocationRow(
    entity: MockLocationEntity,
    onItemClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = entity.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format("Lat: %.4f | Lng: %.4f", entity.latitude, entity.longitude),
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Favorite Button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entity.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (entity.isFavorite) Color(0xFFFBBF24) else Color(0xFF64748B)
                    )
                }

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 3 SCREEN: DEVELOPER CONFIG STEP-BY-STEP
// ==========================================
@Composable
fun DeveloperGuideTab() {
    val steps = listOf(
        Triple(
            "الخطوة الأولى: تفعيل خيارات المطور",
            "Enable Developer Options",
            "افتح إعدادات هاتفك (Settings) ➔ حول الهاتف (About Phone) ➔ اضغط على 'رقم الإصدار' (Build Number) 7 مرات متتالية حتى يُكتب لك 'لقد أصبحت مطور برامج!'."
        ),
        Triple(
            "الخطوة الثانية: اختيار التطبيق الوهمي",
            "Select Mock Location App",
            "عد إلى الإعدادات الرئيسية ➔ النظام ➔ خيارات مطور البرامج (Developer Options) ➔ ابحث عن خيار 'تطبيق الموقع الوهمي' (Select Mock Location App) ➔ اختر تطبيقنا [GPS Spoofer]."
        ),
        Triple(
            "الخطوة الثالثة: تشغيل المحاكاة الفورية",
            "Start Spoofing & Open Sites",
            "عد للتطبيق ➔ اختر الدولة المطلوبة من تبويب 'الدول والمواقع' أو أدخل الإحداثيات يدوياً ➔ اضغط على زر 'بدء محاكاة الموقع' الأخضر للتشغيل فوراً وتصفح المواقع بحرية!"
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "دليل التشغيل لتفعيل محاكي الموقع على هاتفك",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "بسبب حماية نظام الأندرويد، لتغيير موقعك بنجاح افتح خيارات المطور بهاتفك واتبع ما يلي:",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(steps.size) { index ->
            val step = steps[index]
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Step Number Icon Badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF0F172A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = step.first,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = step.second,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = step.third,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ملاحظة هامة: لتغيير عنوان IP وتجاوز الحجب بالكامل، ينصح بتشغيل التطبيق جنباً إلى جنب مع أي متصفح آمن أو تطبيق بروكسي، لضمان تشفير بيانات التصفح وتطابق إحداثيات موقعك الجغرافي بشكل مثالي في كل مكان.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
