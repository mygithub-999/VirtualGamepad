package com.example

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GamepadScreen(
    gamepadManager: GamepadManager?,
    isConnected: Boolean
) {
    var leftStickOffset by remember { mutableStateOf(Offset.Zero) }
    var rightStickOffset by remember { mutableStateOf(Offset.Zero) }
    var buttons1 by remember { mutableStateOf(0.toByte()) }
    
    // Joystick constants
    val maxRadius = with(LocalDensity.current) { 50.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .pointerInteropFilter { event ->
                // The Zero-State Reset mechanism
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    leftStickOffset = Offset.Zero
                    rightStickOffset = Offset.Zero
                    buttons1 = 0
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        gamepadManager?.resetState()
                    }
                    return@pointerInteropFilter true
                }
                
                // Track touches (this is simplified for prototype; true multi-touch requires tracking pointer IDs)
                var newLeftOffset = Offset.Zero
                var newRightOffset = Offset.Zero
                var newButtons = 0
                
                // For a true multi-touch gamepad, we would map pointers to regions.
                // Assuming left side of screen is left stick, right side has right stick and buttons.
                
                // Emitting state update
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    gamepadManager?.updateState(
                        buttons1 = newButtons.toByte(),
                        buttons2 = 0,
                        leftX = (newLeftOffset.x / maxRadius * 127).toInt().toByte(),
                        leftY = (newLeftOffset.y / maxRadius * 127).toInt().toByte(),
                        rightX = (newRightOffset.x / maxRadius * 127).toInt().toByte(),
                        rightY = (newRightOffset.y / maxRadius * 127).toInt().toByte(),
                        l2 = 0,
                        r2 = 0
                    )
                }
                
                true
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusText = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
                "Bluetooth HID not supported on this Android version"
            } else if (isConnected) {
                "Connected to TV"
            } else {
                "Waiting for Bluetooth..."
            }
            Text(
                text = statusText,
                color = if (isConnected) Color.Green else Color.White,
                modifier = Modifier.padding(16.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Joystick
                JoystickView(
                    modifier = Modifier.size(120.dp),
                    offset = leftStickOffset,
                    onOffsetChanged = { newOffset -> 
                        leftStickOffset = newOffset
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            gamepadManager?.updateState(
                                buttons1 = buttons1,
                                buttons2 = 0,
                                leftX = (newOffset.x / maxRadius * 127).toInt().toByte(),
                                leftY = (newOffset.y / maxRadius * 127).toInt().toByte(),
                                rightX = 0,
                                rightY = 0,
                                l2 = 0,
                                r2 = 0
                            )
                        }
                    }
                )
                
                // Right Action Buttons
                ActionButtons(
                    onButtonDown = { buttonMask ->
                        buttons1 = (buttons1.toInt() or buttonMask).toByte()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            gamepadManager?.updateState(buttons1, 0, 0, 0, 0, 0, 0, 0)
                        }
                    },
                    onButtonUp = { buttonMask ->
                        buttons1 = (buttons1.toInt() and buttonMask.inv()).toByte()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            gamepadManager?.updateState(buttons1, 0, 0, 0, 0, 0, 0, 0)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun JoystickView(
    modifier: Modifier,
    offset: Offset,
    onOffsetChanged: (Offset) -> Unit
) {
    val maxRadius = with(LocalDensity.current) { 50.dp.toPx() }
    var center by remember { mutableStateOf(Offset.Zero) }
    
    Canvas(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                center = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
            }
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val touchX = event.x - center.x
                        val touchY = event.y - center.y
                        val distance = hypot(touchX, touchY)
                        
                        val mappedX: Float
                        val mappedY: Float
                        
                        if (distance > maxRadius) {
                            val angle = atan2(touchY, touchX)
                            mappedX = cos(angle) * maxRadius
                            mappedY = sin(angle) * maxRadius
                        } else {
                            mappedX = touchX
                            mappedY = touchY
                        }
                        
                        onOffsetChanged(Offset(mappedX, mappedY))
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        onOffsetChanged(Offset.Zero)
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Base
        drawCircle(
            color = Color.DarkGray,
            radius = maxRadius,
            style = Stroke(width = 4f)
        )
        // Knob
        drawCircle(
            color = Color.LightGray,
            radius = 30.dp.toPx(),
            center = Offset(size.width / 2 + offset.x, size.height / 2 + offset.y)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ActionButtons(
    onButtonDown: (Int) -> Unit,
    onButtonUp: (Int) -> Unit
) {
    Box(modifier = Modifier.size(120.dp)) {
        // Y Button (Top)
        GamepadButton(
            modifier = Modifier.align(Alignment.TopCenter),
            text = "Y",
            mask = 0x08,
            onDown = { onButtonDown(0x08) },
            onUp = { onButtonUp(0x08) }
        )
        // X Button (Left)
        GamepadButton(
            modifier = Modifier.align(Alignment.CenterStart),
            text = "X",
            mask = 0x04,
            onDown = { onButtonDown(0x04) },
            onUp = { onButtonUp(0x04) }
        )
        // B Button (Right)
        GamepadButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            text = "B",
            mask = 0x02,
            onDown = { onButtonDown(0x02) },
            onUp = { onButtonUp(0x02) }
        )
        // A Button (Bottom)
        GamepadButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = "A",
            mask = 0x01,
            onDown = { onButtonDown(0x01) },
            onUp = { onButtonUp(0x01) }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GamepadButton(
    modifier: Modifier,
    text: String,
    mask: Int,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .background(if (isPressed) Color.Gray else Color.DarkGray, CircleShape)
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onDown()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        onUp()
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White)
    }
}
