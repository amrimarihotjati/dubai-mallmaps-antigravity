package uk.dubai.mall.maps.construction.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import coil.compose.AsyncImage
import uk.dubai.mall.maps.construction.data.model.AppConfig
import uk.dubai.mall.maps.construction.data.model.Mall
import uk.dubai.mall.maps.construction.ui.components.NativeAdComponent
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Sort
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import uk.dubai.mall.maps.construction.utils.LocationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    config: AppConfig,
    malls: List<Mall>,
    userLocation: Location?,
    onPermissionGranted: () -> Unit,
    onMallClick: (Mall) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortByNearest by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            }
        }
    )

    LaunchedEffect(Unit) {
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    // Filter definitions
    val filters = listOf(
        "Parking" to Icons.Filled.LocalParking,
        "Prayer Room" to Icons.Filled.Place,
        "Metro" to Icons.Filled.DirectionsTransit,
        "Wheelchair" to Icons.Filled.Accessible,
        "WiFi" to Icons.Filled.Wifi,
        "Family" to Icons.Filled.Favorite
    )
    val selectedFilters = remember { mutableStateListOf<String>() }

    val filteredMalls = remember(malls, selectedFilters.toList(), searchQuery) {
        malls.filter { mall ->
            // Check if mall matches ALL selected filters
            val matchesFilters = selectedFilters.all { filterName ->
                when (filterName) {
                    "Parking" -> (mall.parking ?: 0) > 0
                    "Prayer Room" -> mall.facilities?.contains("Prayer Room") == true
                    "Metro" -> mall.metroStation != null
                    "Wheelchair" -> mall.wheelchairAccessible == true || mall.facilities?.contains("Wheelchair Access") == true
                    "WiFi" -> mall.facilities?.contains("Free WiFi") == true
                    "Family" -> mall.familyFriendly == true
                    else -> true
                }
            }
            val matchesSearch = mall.name.contains(searchQuery, ignoreCase = true)
            matchesFilters && matchesSearch
        }.let { filtered ->
            if (sortByNearest && userLocation != null) {
                filtered.sortedBy { mall ->
                    if (mall.latitude != null && mall.longitude != null) {
                        LocationHelper.calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            mall.latitude, mall.longitude
                        )
                    } else {
                        Float.MAX_VALUE
                    }
                }
            } else {
                filtered
            }
        }
    }

    val distances = remember(filteredMalls, userLocation) {
        filteredMalls.associateWith { mall ->
            if (userLocation != null && mall.latitude != null && mall.longitude != null) {
                LocationHelper.calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    mall.latitude, mall.longitude
                ) / 1000f // in KM
            } else null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(uk.dubai.mall.maps.construction.R.string.discover_dubai),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Filter is now inline below search
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(uk.dubai.mall.maps.construction.R.string.search_malls)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Icon Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nearest Sort Button
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (sortByNearest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (sortByNearest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { 
                            if (!sortByNearest && userLocation == null) {
                                onPermissionGranted()
                            }
                            sortByNearest = !sortByNearest 
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Sort, contentDescription = "Nearest", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(uk.dubai.mall.maps.construction.R.string.nearest), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                items(filters) { (name, icon) ->
                    val isSelected = selectedFilters.contains(name)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            if (isSelected) selectedFilters.remove(name) else selectedFilters.add(name)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = name, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            val shuffledMalls = remember(filteredMalls) { filteredMalls.shuffled() }
            val featuredMalls = remember(shuffledMalls) { shuffledMalls.take(3) }
            val pagerState = rememberPagerState(pageCount = { featuredMalls.size })
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Featured Carousel (Random order, Max 3)
                if (featuredMalls.isNotEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .padding(bottom = 16.dp),
                            pageSpacing = 16.dp
                        ) { page ->
                            CarouselCard(mall = featuredMalls[page], onClick = { onMallClick(featuredMalls[page]) })
                        }
                    }
                }
                // All Malls in Grid (Chunked for Native Ads every 6 items)
                if (filteredMalls.isNotEmpty()) {
                    val chunks = filteredMalls.chunked(6)
                    chunks.forEach { chunk ->
                        gridItems(chunk) { mall ->
                            MallCard(mall = mall, distanceKm = distances[mall], onClick = { onMallClick(mall) })
                        }
                        
                        // Insert Native Ad after every multiple of 6 (if chunk is exactly 6)
                        if (chunk.size == 6) {
                            item(span = { GridItemSpan(3) }) {
                                val nativeId = config.admobConfig.nativeId ?: "ca-app-pub-3940256099942544/2247696110" // Test ID fallback
                                NativeAdComponent(
                                    adUnitId = nativeId,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarouselCard(mall: Mall, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "carouselScale")

    Card(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = mall.imageUrl,
                contentDescription = mall.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.9f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(uk.dubai.mall.maps.construction.R.string.featured),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = mall.name,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = mall.getLocalizedDescription(),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 16.dp)
                    )
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Explore", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun MallCard(mall: Mall, distanceKm: Float? = null, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "cardScale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.70f) // Slightly taller for better elegance
            .scale(scale)
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = mall.imageUrl,
                contentDescription = mall.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Premium Dark Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Distance Badge (Top Right)
            if (distanceKm != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f km", distanceKm),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        )
                    }
                }
            }

            // Content at the bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = mall.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mall.getLocalizedDescription(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward, 
                            contentDescription = "Details", 
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}
