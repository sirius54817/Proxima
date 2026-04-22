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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.components.PinKeypad
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
        val host = activity
        val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
        if (host == null) {
            error = "Biometric unavailable"
            showPin = true
            return
        }

        val canAuth = BiometricManager.from(context)
            .canAuthenticate(allowedAuthenticators)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            error = when (canAuth) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric enrolled on this phone"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "This phone has no biometric hardware"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable"
                else -> "Biometric unavailable"
            }
            showPin = true
            return
        }

        error = null
        showPin = false

        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error = errString.toString()
                    showPin = true
                }

                override fun onAuthenticationFailed() {
                    error = "Biometric not recognized"
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Proxima")
                .setSubtitle("Confirm your identity")
                .setAllowedAuthenticators(allowedAuthenticators)
                .build()
        )
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
                shakeSignal = shakeSignal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        visibleError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        if (biometricEnabled && showPin) {
            TextButton(onClick = { triggerBiometric() }) {
                Text("Use Biometric")
            }
        }

        if (biometricEnabled && !showPin) {
            TextButton(onClick = { showPin = true }) {
                Text("Use PIN instead")
            }
        }
    }
}

