package com.sirius.proxima.data.sis

interface SisSessionProvider {
    suspend fun login(registerNo: String, password: String): SisResult<Unit>
    suspend fun fetch(url: String, headers: Map<String, String> = emptyMap()): SisResult<String>
    suspend fun clearSession()
}


