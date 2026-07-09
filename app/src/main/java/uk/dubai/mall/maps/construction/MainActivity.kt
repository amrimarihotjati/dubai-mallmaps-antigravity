package uk.dubai.mall.maps.construction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import uk.dubai.mall.maps.construction.data.network.ApiService
import uk.dubai.mall.maps.construction.data.repository.MallRepository
import uk.dubai.mall.maps.construction.theme.DubaiMallMapsTheme
import uk.dubai.mall.maps.construction.ui.screens.AboutScreen
import uk.dubai.mall.maps.construction.ui.screens.DetailScreen
import uk.dubai.mall.maps.construction.ui.screens.FavoriteScreen
import uk.dubai.mall.maps.construction.ui.screens.ListScreen
import uk.dubai.mall.maps.construction.ui.screens.OnboardingScreen
import uk.dubai.mall.maps.construction.ui.screens.TripScreen
import uk.dubai.mall.maps.construction.ui.viewmodel.MallViewModel
import uk.dubai.mall.maps.construction.utils.LocationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Initialize AdMob SDK
        MobileAds.initialize(this) {}
        val apiService = ApiService.create()
        val repository = MallRepository(apiService, applicationContext)
        val locationHelper = LocationHelper(this)
        
        setContent {
            val viewModel: MallViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MallViewModel(repository, locationHelper, applicationContext) as T
                    }
                }
            )
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            DubaiMallMapsTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MallViewModel) {
    val navController = rememberNavController()
    val config by viewModel.config.collectAsState()
    val malls by viewModel.malls.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == "home" || currentRoute == "trip" || currentRoute == "favorite" || currentRoute == "about") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.home)) },
                        label = { Text(stringResource(R.string.home)) },
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Place, contentDescription = "Trip") },
                        label = { Text("Trip") },
                        selected = currentRoute == "trip",
                        onClick = {
                            navController.navigate("trip") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.favorite)) },
                        label = { Text(stringResource(R.string.favorite)) },
                        selected = currentRoute == "favorite",
                        onClick = {
                            navController.navigate("favorite") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.about)) },
                        label = { Text(stringResource(R.string.about)) },
                        selected = currentRoute == "about",
                        onClick = {
                            navController.navigate("about") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
            }
        } else if (config != null) {
            val startDest = if (isOnboardingCompleted) "home" else "onboarding"
            NavHost(
                navController = navController,
                startDestination = startDest,
                modifier = Modifier.padding(paddingValues),
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left, tween(300)
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left, tween(300)
                    )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right, tween(300)
                    )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right, tween(300)
                    )
                }
            ) {
                composable("onboarding") {
                    OnboardingScreen(
                        onFinish = {
                            viewModel.completeOnboarding()
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
                composable("trip") {
                    val tripMalls by viewModel.tripMalls.collectAsState()
                    TripScreen(
                        tripMalls = tripMalls,
                        onMallClick = { mall ->
                            navController.navigate("detail/${mall.id}")
                        },
                        onRemoveClick = { mall ->
                            viewModel.toggleTripMall(mall.id)
                        }
                    )
                }
                composable("home") {
                    ListScreen(
                        config = config!!,
                        malls = malls,
                        userLocation = userLocation,
                        onPermissionGranted = { viewModel.fetchUserLocation() },
                        onMallClick = { mall ->
                            navController.navigate("detail/${mall.id}")
                        }
                    )
                }
                composable("favorite") {
                    FavoriteScreen(
                        malls = malls,
                        onMallClick = { mall ->
                            navController.navigate("detail/${mall.id}")
                        }
                    )
                }
                composable("about") {
                    val isDarkMode by viewModel.isDarkMode.collectAsState()
                    AboutScreen(
                        isDarkMode = isDarkMode,
                        onThemeToggle = { viewModel.toggleTheme() }
                    )
                }
                composable(
                    "detail/{mallId}",
                    arguments = listOf(androidx.navigation.navArgument("mallId") { type = androidx.navigation.NavType.IntType })
                ) { backStackEntry ->
                    val mallId = backStackEntry.arguments?.getInt("mallId")
                    val mall = malls.find { it.id == mallId }
                    
                    if (mall != null) {
                        val tripMalls by viewModel.tripMalls.collectAsState()
                        val isTripMall = tripMalls.any { it.id == mall.id }
                        DetailScreen(
                            mall = mall,
                            config = config!!,
                            isTripMall = isTripMall,
                            onBackClick = { 
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(mall.id) },
                            onTripClick = { viewModel.toggleTripMall(mall.id) }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}
