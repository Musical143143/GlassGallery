package com.example.glassgallery.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.glassgallery.ui.glass.GlassBottomBar
import com.example.glassgallery.ui.glass.GlassTab
import com.example.glassgallery.ui.screens.AlbumDetailScreen
import com.example.glassgallery.ui.screens.AlbumsScreen
import com.example.glassgallery.ui.screens.PhotosScreen
import com.example.glassgallery.ui.screens.ViewerScreen

object Routes {
    const val PHOTOS = "photos"
    const val ALBUMS = "albums"
    const val ALBUM = "album/{bucketId}"
    const val VIEWER = "viewer/{key}"
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
fun GlassGalleryApp(vm: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            requiredPermissions().any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            },
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = result.values.any { it }
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(requiredPermissions())
    }
    LaunchedEffect(granted) {
        if (granted) vm.load()
    }

    if (!granted) {
        PermissionPrompt(onGrant = { permissionLauncher.launch(requiredPermissions()) })
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.PHOTOS,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 8 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 8 } },
        ) {
            composable(Routes.PHOTOS) {
                PhotosScreen(
                    vm = vm,
                    onOpen = { items, key ->
                        vm.openViewer(items)
                        navController.navigate("viewer/$key")
                    },
                )
            }
            composable(Routes.ALBUMS) {
                AlbumsScreen(
                    vm = vm,
                    onAlbum = { bucketId -> navController.navigate("album/$bucketId") },
                )
            }
            composable(
                route = Routes.ALBUM,
                arguments = listOf(navArgument("bucketId") { type = NavType.LongType }),
            ) { entry ->
                val bucketId = entry.arguments?.getLong("bucketId") ?: 0L
                AlbumDetailScreen(
                    vm = vm,
                    bucketId = bucketId,
                    onBack = { navController.popBackStack() },
                    onOpen = { items, key ->
                        vm.openViewer(items)
                        navController.navigate("viewer/$key")
                    },
                )
            }
            composable(
                route = Routes.VIEWER,
                arguments = listOf(navArgument("key") { type = NavType.StringType }),
            ) { entry ->
                ViewerScreen(
                    vm = vm,
                    startKey = entry.arguments?.getString("key").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }

        val showTabs = route == Routes.PHOTOS || route == Routes.ALBUMS
        AnimatedVisibility(
            visible = showTabs,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            GlassBottomBar(
                selected = if (route == Routes.ALBUMS) GlassTab.ALBUMS else GlassTab.PHOTOS,
                onSelect = { tab ->
                    val target = if (tab == GlassTab.ALBUMS) Routes.ALBUMS else Routes.PHOTOS
                    navController.navigate(target) {
                        popUpTo(Routes.PHOTOS) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Glass Gallery needs access to your photos and videos.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGrant) {
            Text("Allow access")
        }
    }
}
