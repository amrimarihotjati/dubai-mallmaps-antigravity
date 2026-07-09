package uk.dubai.mall.maps.construction.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import uk.dubai.mall.maps.construction.data.model.AppConfig
import uk.dubai.mall.maps.construction.data.model.Mall
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    mall: Mall,
    config: AppConfig,
    isTripMall: Boolean = false,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onTripClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    var showFloorPlan by remember { mutableStateOf(false) }
    val isFavorite = mall.isFavorite ?: false
    
    LaunchedEffect(config.admobConfig.interstitialId) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            config.admobConfig.interstitialId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
            }
        )
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(mall.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Hey, check out ${mall.name}! Explore it here: ${mall.gmapsLink}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onTripClick) {
                        Icon(
                            imageVector = if (isTripMall) Icons.Filled.WrongLocation else Icons.Filled.AddLocation,
                            contentDescription = "Trip",
                            tint = if (isTripMall) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Image or Gallery
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp) // Fixed sensible height instead of fillMaxHeight
                ) {
                    if (!mall.galleryImages.isNullOrEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { mall.galleryImages.size })
                        val coroutineScope = rememberCoroutineScope()
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = mall.galleryImages[page],
                                contentDescription = "${mall.name} image ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // Gallery Controls Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f))
                        ) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(if (pagerState.currentPage > 0) pagerState.currentPage - 1 else mall.galleryImages.size - 1)
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterStart).padding(16.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous", tint = Color.White)
                            }
                            
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(if (pagerState.currentPage < mall.galleryImages.size - 1) pagerState.currentPage + 1 else 0)
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = Color.White)
                            }

                            // Pager Indicators
                            Row(
                                Modifier
                                    .height(50.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 48.dp), // higher because of surface overlap
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(mall.galleryImages.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        AsyncImage(
                            model = mall.imageUrl,
                            contentDescription = mall.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }

                // Content Bottom Sheet (Overlapping the image slightly)
                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-32).dp) // Overlap the image
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            // padding bottom to account for the -32dp offset
                            .padding(bottom = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                .align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(uk.dubai.mall.maps.construction.R.string.overview),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = mall.name,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Tags
                        if (!mall.tags.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(mall.tags) { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            text = tag,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Stats Row
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (mall.rating != null) {
                                val formattedRating = String.format("%.1f", mall.rating)
                                StatChip(icon = Icons.Filled.Star, label = "$formattedRating (${mall.reviewCount ?: 0})", color = Color(0xFFFFC107))
                            }
                            if (mall.stores != null) {
                                StatChip(icon = Icons.Filled.Storefront, label = "${mall.stores}+ Stores", color = MaterialTheme.colorScheme.secondary)
                            }
                            if (mall.parking != null) {
                                StatChip(icon = Icons.Filled.LocalParking, label = "${mall.parking} Spots", color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = mall.getLocalizedDescription(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Info List
                        if (mall.address != null) {
                            InfoRow(icon = Icons.Filled.LocationOn, text = mall.address)
                        }
                        if (mall.openingHours != null) {
                            InfoRow(icon = Icons.Filled.AccessTime, text = mall.openingHours)
                        }
                        if (mall.metroStation != null) {
                            InfoRow(icon = Icons.Filled.DirectionsTransit, text = mall.metroStation)
                        }
                        if (mall.phone != null) {
                            InfoRow(icon = Icons.Filled.Phone, text = mall.phone)
                        }
                        if (mall.website != null) {
                            InfoRow(icon = Icons.Filled.Language, text = mall.website)
                        }
                        
                        // Facilities
                        if (!mall.facilities.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(uk.dubai.mall.maps.construction.R.string.facilities), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(mall.facilities) { facility ->
                                    StatChip(icon = Icons.Filled.CheckCircle, label = facility, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        
                        // Anchor Stores
                        if (!mall.anchorStores.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(uk.dubai.mall.maps.construction.R.string.anchor_stores),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(mall.anchorStores) { store ->
                                    StatChip(icon = Icons.Filled.ShoppingBag, label = store, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                val showAdAndNavigate = {
                                    openGoogleMaps(context, mall.gmapsLink)
                                }
                                
                                if (interstitialAd != null) {
                                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                        override fun onAdDismissedFullScreenContent() {
                                            interstitialAd = null
                                            showAdAndNavigate()
                                        }
                                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                            interstitialAd = null
                                            showAdAndNavigate()
                                        }
                                    }
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        interstitialAd?.show(activity)
                                    } else {
                                        showAdAndNavigate()
                                    }
                                } else {
                                    showAdAndNavigate()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                stringResource(uk.dubai.mall.maps.construction.R.string.open_gmaps),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        // Public Transport Button
                        if (mall.latitude != null && mall.longitude != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    val showAdAndNavigate = {
                                        openGoogleMapsTransit(context, mall.latitude, mall.longitude)
                                    }
                                    
                                    if (interstitialAd != null) {
                                        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                            override fun onAdDismissedFullScreenContent() {
                                                interstitialAd = null
                                                showAdAndNavigate()
                                            }
                                            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                                interstitialAd = null
                                                showAdAndNavigate()
                                            }
                                        }
                                        val activity = context as? android.app.Activity
                                        if (activity != null) {
                                            interstitialAd?.show(activity)
                                        } else {
                                            showAdAndNavigate()
                                        }
                                    } else {
                                        showAdAndNavigate()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Filled.DirectionsTransit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(
                                    stringResource(uk.dubai.mall.maps.construction.R.string.transit_directions),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        
                        // Floor Plan Button
                        if (mall.floorPlanUrl != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showFloorPlan = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(
                                    "View Floor Plan",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Reviews Section
                        Text("Reviews", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        MockReviewCard(name = "Sarah M.", rating = 5, comment = "An absolutely amazing shopping experience! The indoor aquarium is breathtaking.", time = "2 days ago")
                        Spacer(modifier = Modifier.height(12.dp))
                        MockReviewCard(name = "Ahmad R.", rating = 4, comment = "Great place but can get very crowded on weekends. Lots of parking though.", time = "1 week ago")
                        Spacer(modifier = Modifier.height(12.dp))
                        MockReviewCard(name = "Elena K.", rating = 5, comment = "The fountain show outside is spectacular. So many luxury brands in one place.", time = "2 weeks ago")
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
    
    if (showFloorPlan && mall.floorPlanUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFloorPlan = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                
                AsyncImage(
                    model = mall.floorPlanUrl,
                    contentDescription = "Floor Plan",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale > 1f) {
                                    offset + pan
                                } else {
                                    androidx.compose.ui.geometry.Offset.Zero
                                }
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
                
                IconButton(
                    onClick = { showFloorPlan = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatChip(icon: ImageVector, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MockReviewCard(name: String, rating: Int, comment: String, time: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar Placeholder
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name.take(1), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    repeat(rating) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun openGoogleMaps(context: Context, link: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openGoogleMapsTransit(context: Context, lat: Double, lng: Double) {
    try {
        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=transit")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to web browser if Maps is not installed
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=transit")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
