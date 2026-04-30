package com.sirius.proxima.ui.screen.security

import android.app.Application
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.components.PinKeypad
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.util.BiometricUtils
import com.sirius.proxima.viewmodel.SecurityViewModel

@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    viewModel: SecurityViewModel = viewModel(
        factory = SecurityViewModel.factory(LocalContext.current.applicationContext as Application)
    )
) {
    BackHandler { }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()

    var error by remember { mutableStateOf<String?>(null) }
    var clearSignal by remember { mutableIntStateOf(0) }
    var shakeSignal by remember { mutableIntStateOf(0) }
    var showPin by remember { mutableStateOf(!biometricEnabled) }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    fun triggerBiometric() {
        if (activity == null) {
            error = "Biometric unavailable"
            showPin = true
            return
        }

        if (!BiometricUtils.isBiometricAvailable(context)) {
            error = "Biometric not enrolled or unavailable"
            showPin = true
            return
        }

        error = null
        showPin = false

        BiometricUtils.showBiometricPrompt(
            activity = activity,
            onSuccess = {
                onUnlocked()
            },
            onFailure = { err ->
                error = err
                // If the user cancels or uses PIN, show the PIN entry
                if (err?.contains("cancel", ignoreCase = true) == true ||
                    err?.contains("pin", ignoreCase = true) == true) {
                    showPin = true
                }
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, biometricEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && biometricEnabled) {
                triggerBiometric()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            triggerBiometric()
        } else {
            showPin = true
        }
    }

    val visibleError = error?.takeUnless {
        it.contains("biometric", ignoreCase = true) ||
            it.contains("fingerprint", ignoreCase = true) ||
            it.contains("face", ignoreCase = true)
    }

    AppLockContent(
        showPin = showPin,
        biometricEnabled = biometricEnabled,
        visibleError = visibleError,
        onTriggerBiometric = { triggerBiometric() },
        onPinComplete = { pin ->
            error = null
            if (viewModel.verifyPin(pin)) {
                onUnlocked()
            } else {
                error = "Incorrect PIN"
                shakeSignal += 1
                clearSignal += 1
            }
        },
        clearSignal = clearSignal,
        shakeSignal = shakeSignal,
        onUsePinInstead = { showPin = true }
    )
}

@Composable
private fun AppLockContent(
    showPin: Boolean,
    biometricEnabled: Boolean,
    visibleError: String?,
    onTriggerBiometric: () -> Unit,
    onPinComplete: (String) -> Unit,
    clearSignal: Int,
    shakeSignal: Int,
    onUsePinInstead: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PROXIMA",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter PIN to continue",
            color = Color(0xFFA1A1AA),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (showPin) {
            PinKeypad(
                onPinComplete = onPinComplete,
                clearSignal = clearSignal,
                shakeSignal = shakeSignal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        visibleError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        if (biometricEnabled && showPin) {
            TextButton(onClick = onTriggerBiometric) {
                Text("Use Biometric")
            }
        }

        if (biometricEnabled && !showPin) {
            TextButton(onClick = onUsePinInstead) {
                Text("Use PIN instead")
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF09090B)
@Composable
fun AppLockScreenPreview() {
    ProximaTheme {
        AppLockContent(
            showPin = true,
            biometricEnabled = true,
            visibleError = "Incorrect PIN",
            onTriggerBiometric = {},
            onPinComplete = {},
            clearSignal = 0,
            shakeSignal = 0,
            onUsePinInstead = {}
        )
    }
}

