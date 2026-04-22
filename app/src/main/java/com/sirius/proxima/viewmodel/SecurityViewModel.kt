package com.sirius.proxima.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sirius.proxima.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val securityManager = SecurityManager(application.applicationContext)

    private val _hasPin = MutableStateFlow(false)
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(false)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _backupLockEnabled = MutableStateFlow(false)
    val backupLockEnabled: StateFlow<Boolean> = _backupLockEnabled.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        _hasPin.value = securityManager.hasPin()
        _appLockEnabled.value = securityManager.isAppLockEnabled()
        _biometricEnabled.value = securityManager.isBiometricEnabled()
        _backupLockEnabled.value = securityManager.isBackupLockEnabled()
    }

    fun verifyPin(pin: String): Boolean {
        return securityManager.verifyPin(pin)
    }

    fun setupPin(pin: String) {
        securityManager.setupPin(pin)
        refresh()
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!securityManager.verifyPin(currentPin)) return false
        securityManager.changePin(newPin)
        refresh()
        return true
    }

    fun setAppLockEnabled(enabled: Boolean) {
        securityManager.setAppLockEnabled(enabled)
        refresh()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        refresh()
    }

    fun setBackupLockEnabled(enabled: Boolean) {
        securityManager.setBackupLockEnabled(enabled)
        refresh()
    }

    fun clearPin() {
        securityManager.clearPin()
        refresh()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext as Application
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return SecurityViewModel(appContext) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

