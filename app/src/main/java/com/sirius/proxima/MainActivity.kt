package com.sirius.proxima

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirius.proxima.di.ServiceLocator
import com.sirius.proxima.security.SecurityManager
import com.sirius.proxima.ui.navigation.ProximaNavGraph
import com.sirius.proxima.ui.screen.security.AppLockScreen
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.ui.theme.ThemeMode
import com.sirius.proxima.worker.BackupScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val securityManager by lazy { SecurityManager(applicationContext) }
    private var isLocked by mutableStateOf(false)
    private var appLockEnabled by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()
        BackupScheduler.scheduleDailyBackup(this)
        val settingsDataStore = ServiceLocator.getSettingsDataStore(applicationContext)

        appLockEnabled = securityManager.isAppLockEnabled()
        isLocked = appLockEnabled

        setContent {
            val appThemeMode = settingsDataStore.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM).value
            val useMaterial3 = settingsDataStore.useMaterial3.collectAsStateWithLifecycle(initialValue = false).value
            val useMaterialYou = settingsDataStore.useMaterialYou.collectAsStateWithLifecycle(initialValue = false).value
            var showSplash by remember { mutableStateOf(true) }

            ProximaTheme(
                themeMode = appThemeMode,
                useMaterial3 = useMaterial3,
                useMaterialYou = useMaterialYou
            ) {
                when {
                    appLockEnabled && isLocked -> {
                        AppLockScreen(onUnlocked = { isLocked = false })
                    }
                    showSplash -> {
                        ProximaSplash(onFinished = { showSplash = false })
                    }
                    else -> {
                        ProximaNavGraph()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLockEnabled = securityManager.isAppLockEnabled()
    }

    override fun onStop() {
        super.onStop()
        if (securityManager.isAppLockEnabled()) {
            appLockEnabled = true
            isLocked = true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun ProximaSplash(onFinished: () -> Unit) {
    val letters = "PROXIMA"
    val offsets = remember { letters.map { Animatable(70f) } }
    val alphas = remember { letters.map { Animatable(0f) } }

    LaunchedEffect(Unit) {
        letters.indices.forEach { index ->
            launch {
                delay(index * 55L)
                launch {
                    offsets[index].animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    alphas[index].animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 330)
                    )
                }
            }
        }
        delay(letters.length * 55L + 900L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            letters.forEachIndexed { index, letter ->
                androidx.compose.material3.Text(
                    text = letter.toString(),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = alphas[index].value),
                    letterSpacing = 6.sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = offsets[index].value
                    }
                )
            }
        }
    }
}

