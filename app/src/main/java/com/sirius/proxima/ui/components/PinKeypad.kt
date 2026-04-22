package com.sirius.proxima.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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

@Composable
fun PinKeypad(
    onPinComplete: (String) -> Unit,
    clearSignal: Int,
    shakeSignal: Int,
    modifier: Modifier = Modifier
) {
    var digits by remember { mutableStateOf(listOf<Int>()) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(clearSignal) {
        digits = emptyList()
    }

    LaunchedEffect(shakeSignal) {
        if (shakeSignal == 0) return@LaunchedEffect
        repeat(3) {
            shakeOffset.animateTo(-14f, animationSpec = tween(45))
            shakeOffset.animateTo(14f, animationSpec = tween(45))
        }
        shakeOffset.animateTo(0f, animationSpec = tween(45))
    }

    LaunchedEffect(digits) {
        if (digits.size == 6) {
            onPinComplete(digits.joinToString(separator = ""))
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.graphicsLayer { translationX = shakeOffset.value },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(6) { index ->
                val filled = index < digits.size
                val fillAlpha by animateFloatAsState(
                    targetValue = if (filled) 1f else 0f,
                    animationSpec = tween(150),
                    label = "dotFill"
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        .background(Color.White.copy(alpha = fillAlpha), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.size(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PinRow(keys = listOf("1", "2", "3"), onKey = { key ->
                if (digits.size < 6) digits = digits + key.toInt()
            })
            PinRow(keys = listOf("4", "5", "6"), onKey = { key ->
                if (digits.size < 6) digits = digits + key.toInt()
            })
            PinRow(keys = listOf("7", "8", "9"), onKey = { key ->
                if (digits.size < 6) digits = digits + key.toInt()
            })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
            ) {
                Spacer(modifier = Modifier.size(72.dp))
                NumberKey(text = "0") {
                    if (digits.size < 6) digits = digits + 0
                }
                BackspaceKey {
                    if (digits.isNotEmpty()) digits = digits.dropLast(1)
                }
            }
        }
    }
}

@Composable
private fun PinRow(
    keys: List<String>,
    onKey: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
    ) {
        keys.forEach { key ->
            NumberKey(text = key) { onKey(key) }
        }
    }
}

@Composable
private fun NumberKey(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(90),
        label = "keyScale"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(Color(0xFF27272A), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BackspaceKey(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(90),
        label = "keyScaleBack"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(Color(0xFF27272A), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Backspace,
            contentDescription = "Backspace",
            tint = Color.White
        )
    }
}

