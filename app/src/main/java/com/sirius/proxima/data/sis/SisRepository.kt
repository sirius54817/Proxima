package com.sirius.proxima.data.sis

import com.sirius.proxima.data.datastore.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SisRepository(
    private val scraper: SISScraper,
    private val settingsDataStore: SettingsDataStore
) {
    suspend fun login(registerNo: String, password: String): SisResult<Unit> =
        withContext(Dispatchers.IO) {
            val result = scraper.login(registerNo, password)
            if (result is SisResult.Success) {
                settingsDataStore.setSisCredentials(registerNo, password)
            }
            result
        }

    suspend fun getAttendance(): SisResult<List<SisAttendance>> =
        withContext(Dispatchers.IO) {
            scraper.getAttendance()
        }

    suspend fun loginAndFetch(registerNo: String, password: String): SisResult<List<SisAttendance>> =
        withContext(Dispatchers.IO) {
            val loginResult = scraper.login(registerNo, password)
            if (loginResult is SisResult.Error) return@withContext loginResult
            settingsDataStore.setSisCredentials(registerNo, password)
            scraper.getAttendance()
        }

    suspend fun logout() {
        settingsDataStore.clearSisCredentials()
    }
}

