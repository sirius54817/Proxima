package com.sirius.proxima.ui.components

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DeleteAnimationBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    fun trigger() {
        _events.tryEmit(Unit)
    }
}

