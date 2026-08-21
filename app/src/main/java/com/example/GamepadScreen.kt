package com.example

// Added to force platform VFS sync
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun GamepadScreen(
    gamepadManager: GamepadManager?,
    isConnected: Boolean,
    onMakeDiscoverable: () -> Unit
) {
    var buttons1 by remember { mutableStateOf(0.toByte()) }
    var buttons2 by remember { mutableStateOf(0.toByte()) }
    var leftX by remember { mutableStateOf(0.toByte()) }
    var leftY by remember { mutableStateOf(0.toByte()) }

    fun sendState() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            gamepadManager?.updateState(buttons1, buttons2, leftX, leftY, 0, 0, 0, 0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Status and Connect Button (Top Center)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isConnected) "Connected" else "Disconnected",
                color = if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            if (!isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onMakeDiscoverable,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                ) {
                    Text("Make Discoverable (Pair TV)")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // D-Pad (Left)
            DPad(
                onDirectionChanged = { x, y ->
                    leftX = x
                    leftY = y
                    sendState()
                }
            )

            // Action Buttons (Right)
            ActionButtons(
                onButtonDown = { mask ->
                    buttons1 = (buttons1.toInt() or mask).toByte()
                    sendState()
                },
                onButtonUp = { mask ->
                    buttons1 = (buttons1.toInt() and mask.inv()).toByte()
                    sendState()
                }
            )
        }
    }
}

@Composable
fun DPad(onDirectionChanged: (Byte, Byte) -> Unit) {
    val dpadSize = 160.dp
    val buttonSize = 50.dp
    
    Box(
        modifier = Modifier.size(dpadSize),
        contentAlignment = Alignment.Center
    ) {
        // Up
        DPadButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(buttonSize),
            onDown = { onDirectionChanged(0, -127) },
            onUp = { onDirectionChanged(0, 0) }
        )
        // Down
        DPadButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(buttonSize),
            onDown = { onDirectionChanged(0, 127) },
            onUp = { onDirectionChanged(0, 0) }
        )
        // Left
        DPadButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(buttonSize),
            onDown = { onDirectionChanged(-127, 0) },
            onUp = { onDirectionChanged(0, 0) }
        )
        // Right
        DPadButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(buttonSize),
            onDown = { onDirectionChanged(127, 0) },
            onUp = { onDirectionChanged(0, 0) }
        )
        // Center decorative
        Box(
            modifier = Modifier
                .size(buttonSize)
                .background(Color(0xFF333333))
        )
    }
}

@Composable
fun DPadButton(modifier: Modifier, onDown: () -> Unit, onUp: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .background(if (isPressed) Color(0xFF555555) else Color(0xFF333333), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            }
    )
}

@Composable
fun ActionButtons(
    onButtonDown: (Int) -> Unit,
    onButtonUp: (Int) -> Unit
) {
    val padSize = 160.dp
    val buttonSize = 50.dp
    
    Box(
        modifier = Modifier.size(padSize),
        contentAlignment = Alignment.Center
    ) {
        // Y Button (Top)
        GamepadButton(
            modifier = Modifier.align(Alignment.TopCenter),
            text = "Y",
            color = Color(0xFFFFC107), // Yellow
            size = buttonSize,
            onDown = { onButtonDown(0x08) },
            onUp = { onButtonUp(0x08) }
        )
        // X Button (Left)
        GamepadButton(
            modifier = Modifier.align(Alignment.CenterStart),
            text = "X",
            color = Color(0xFF2196F3), // Blue
            size = buttonSize,
            onDown = { onButtonDown(0x04) },
            onUp = { onButtonUp(0x04) }
        )
        // B Button (Right)
        GamepadButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            text = "B",
            color = Color(0xFFF44336), // Red
            size = buttonSize,
            onDown = { onButtonDown(0x02) },
            onUp = { onButtonUp(0x02) }
        )
        // A Button (Bottom)
        GamepadButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = "A",
            color = Color(0xFF4CAF50), // Green
            size = buttonSize,
            onDown = { onButtonDown(0x01) },
            onUp = { onButtonUp(0x01) }
        )
    }
}

@Composable
fun GamepadButton(
    modifier: Modifier,
    text: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(size)
            .background(if (isPressed) color.copy(alpha = 0.5f) else color, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = Color.White, 
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}
