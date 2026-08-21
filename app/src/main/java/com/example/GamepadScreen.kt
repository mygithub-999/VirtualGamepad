package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import kotlin.math.roundToInt

@Composable
fun GamepadScreen(
    gamepadManager: GamepadManager?,
    isConnected: Boolean,
    onMakeDiscoverable: () -> Unit
) {
    var buttons1 by remember { mutableStateOf(0.toByte()) }
    var buttons2 by remember { mutableStateOf(0.toByte()) }
    var hat by remember { mutableStateOf(8.toByte()) }
    var leftX by remember { mutableStateOf(0.toByte()) }
    var leftY by remember { mutableStateOf(0.toByte()) }
    var rightX by remember { mutableStateOf(0.toByte()) }
    var rightY by remember { mutableStateOf(0.toByte()) }
    var l2 by remember { mutableStateOf(0.toByte()) }
    var r2 by remember { mutableStateOf(0.toByte()) }

    fun sendState() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            gamepadManager?.updateState(buttons1, buttons2, hat, leftX, leftY, rightX, rightY, l2, r2)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF262423)) // Match the warm dark grey from the image
    ) {
        // Top Bumpers Row (L2, L1 ... R1, R2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Bumpers
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BumperButton(
                    text = "L2",
                    onDown = { l2 = 255.toByte(); sendState() },
                    onUp = { l2 = 0.toByte(); sendState() }
                )
                BumperButton(
                    text = "L1",
                    onDown = { buttons1 = (buttons1.toInt() or 0x10).toByte(); sendState() },
                    onUp = { buttons1 = (buttons1.toInt() and 0x10.inv()).toByte(); sendState() }
                )
            }
            // Right Bumpers
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BumperButton(
                    text = "R1",
                    onDown = { buttons1 = (buttons1.toInt() or 0x20).toByte(); sendState() },
                    onUp = { buttons1 = (buttons1.toInt() and 0x20.inv()).toByte(); sendState() }
                )
                BumperButton(
                    text = "R2",
                    onDown = { r2 = 255.toByte(); sendState() },
                    onUp = { r2 = 0.toByte(); sendState() }
                )
            }
        }

        // Status and Connect Button
        if (!isConnected) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onMakeDiscoverable,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                ) {
                    Text("Make Discoverable (Pair TV)")
                }
            }
        }

        // Center Menu Buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Home Button
            MenuIconButton(
                icon = Icons.Default.Home,
                size = 56.dp,
                onDown = {
                    buttons2 = (buttons2.toInt() or 0x10).toByte()
                    sendState()
                },
                onUp = {
                    buttons2 = (buttons2.toInt() and 0x10.inv()).toByte()
                    sendState()
                }
            )
            
            // Row of three buttons: Select/View, Share, Menu
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                MenuTextButton(
                    text = "□", // Select / Back (Button 9)
                    onDown = {
                        buttons2 = (buttons2.toInt() or 0x01).toByte()
                        sendState()
                    },
                    onUp = {
                        buttons2 = (buttons2.toInt() and 0x01.inv()).toByte()
                        sendState()
                    }
                )
                MenuIconButton(
                    icon = Icons.Default.KeyboardArrowUp, // Share (Button 14)
                    size = 40.dp,
                    onDown = {
                        buttons2 = (buttons2.toInt() or 0x20).toByte()
                        sendState()
                    },
                    onUp = {
                        buttons2 = (buttons2.toInt() and 0x20.inv()).toByte()
                        sendState()
                    }
                )
                MenuIconButton(
                    icon = Icons.Default.Menu, // Start (Button 10)
                    size = 40.dp,
                    onDown = {
                        buttons2 = (buttons2.toInt() or 0x02).toByte()
                        sendState()
                    },
                    onUp = {
                        buttons2 = (buttons2.toInt() and 0x02.inv()).toByte()
                        sendState()
                    }
                )
            }
        }

        // Left Side Controls (Thumbstick + D-Pad)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Thumbstick(
                modifier = Modifier.size(120.dp),
                onUpdate = { x, y ->
                    leftX = x
                    leftY = y
                    sendState()
                },
                onClick = {
                    // L3 (Button 11)
                    buttons2 = (buttons2.toInt() or 0x04).toByte()
                    sendState()
                },
                onRelease = {
                    buttons2 = (buttons2.toInt() and 0x04.inv()).toByte()
                    sendState()
                }
            )
            DPad(
                modifier = Modifier.size(140.dp),
                onUpdate = { h ->
                    hat = h
                    sendState()
                }
            )
        }

        // Right Side Controls (Action Buttons + Right Thumbstick)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp, top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
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
            Thumbstick(
                modifier = Modifier.size(120.dp),
                onUpdate = { x, y ->
                    rightX = x
                    rightY = y
                    sendState()
                },
                onClick = {
                    // R3 (Button 12)
                    buttons2 = (buttons2.toInt() or 0x08).toByte()
                    sendState()
                },
                onRelease = {
                    buttons2 = (buttons2.toInt() and 0x08.inv()).toByte()
                    sendState()
                }
            )
        }
    }
}

@Composable
fun BumperButton(text: String, onDown: () -> Unit, onUp: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 40.dp)
            .background(if (isPressed) Color(0xFF555352) else Color(0xFF383534), RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun MenuIconButton(icon: ImageVector, size: Dp, onDown: () -> Unit, onUp: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    Box(
        modifier = Modifier
            .size(size)
            .background(if (isPressed) Color(0xFF555352) else Color(0xFF383534), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun MenuTextButton(text: String, onDown: () -> Unit, onUp: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (isPressed) Color(0xFF555352) else Color(0xFF383534), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Thumbstick(
    modifier: Modifier = Modifier, 
    onUpdate: (Byte, Byte) -> Unit,
    onClick: () -> Unit = {},
    onRelease: () -> Unit = {}
) {
    var stickOffset by remember { mutableStateOf(Offset.Zero) }
    val view = LocalView.current
    
    Box(
        modifier = modifier
            .background(Color(0xFF1E1C1B), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                    
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f
                    
                    fun updateOffset(eventPos: Offset) {
                        var delta = eventPos - center
                        val dist = delta.getDistance()
                        if (dist > maxRadius) {
                            delta *= (maxRadius / dist)
                        }
                        stickOffset = delta
                        
                        val mapX = ((delta.x / maxRadius) * 127).roundToInt().coerceIn(-127, 127).toByte()
                        val mapY = ((delta.y / maxRadius) * 127).roundToInt().coerceIn(-127, 127).toByte()
                        onUpdate(mapX, mapY)
                    }
                    
                    updateOffset(down.position)
                    
                    do {
                        val event = awaitPointerEvent()
                        val current = event.changes.firstOrNull { it.id == down.id }
                        if (current != null) {
                            updateOffset(current.position)
                        }
                    } while (event.changes.any { it.pressed })
                    
                    stickOffset = Offset.Zero
                    onUpdate(0, 0)
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF2C2A29),
                radius = size.minDimension / 2f,
                style = Stroke(width = 6.dp.toPx())
            )
        }
        // Stick head
        Box(
            modifier = Modifier
                .offset(
                    x = with(androidx.compose.ui.platform.LocalDensity.current) { stickOffset.x.toDp() },
                    y = with(androidx.compose.ui.platform.LocalDensity.current) { stickOffset.y.toDp() }
                )
                .size(70.dp)
                .background(Color(0xFF454240), CircleShape)
        ) {
            // Inner stick ridge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(Color.Transparent, CircleShape)
                    .background(Color(0xFF383534), CircleShape)
            )
        }
    }
}

@Composable
fun DPad(modifier: Modifier = Modifier, onUpdate: (Byte) -> Unit) {
    var upPressed by remember { mutableStateOf(false) }
    var downPressed by remember { mutableStateOf(false) }
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }

    fun updateHat() {
        val h = when {
            upPressed && rightPressed -> 1
            downPressed && rightPressed -> 3
            downPressed && leftPressed -> 5
            upPressed && leftPressed -> 7
            upPressed -> 0
            rightPressed -> 2
            downPressed -> 4
            leftPressed -> 6
            else -> 8
        }
        onUpdate(h.toByte())
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1E1C1B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF2C2A29),
                radius = size.minDimension / 2f,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Cross shape
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height
            val thick = w / 3f
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            // Vertical rect
            drawRoundRect(
                color = Color(0xFF383534),
                topLeft = Offset((w - thick) / 2f, 0f),
                size = androidx.compose.ui.geometry.Size(thick, h),
                cornerRadius = cornerRadius
            )
            // Horizontal rect
            drawRoundRect(
                color = Color(0xFF383534),
                topLeft = Offset(0f, (h - thick) / 2f),
                size = androidx.compose.ui.geometry.Size(w, thick),
                cornerRadius = cornerRadius
            )
        }

        val btnSize = modifier.then(Modifier).let { 40.dp } // Simplified sizing

        // Up
        DPadButton(
            modifier = Modifier.align(Alignment.TopCenter).size(btnSize).padding(top = 16.dp),
            onState = { upPressed = it; updateHat() }
        )
        // Down
        DPadButton(
            modifier = Modifier.align(Alignment.BottomCenter).size(btnSize).padding(bottom = 16.dp),
            onState = { downPressed = it; updateHat() }
        )
        // Left
        DPadButton(
            modifier = Modifier.align(Alignment.CenterStart).size(btnSize).padding(start = 16.dp),
            onState = { leftPressed = it; updateHat() }
        )
        // Right
        DPadButton(
            modifier = Modifier.align(Alignment.CenterEnd).size(btnSize).padding(end = 16.dp),
            onState = { rightPressed = it; updateHat() }
        )
    }
}

@Composable
fun DPadButton(modifier: Modifier, onState: (Boolean) -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    Box(
        modifier = modifier
            .background(if (isPressed) Color(0x33FFFFFF) else Color.Transparent)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onState(true)
                    waitForUpOrCancellation()
                    isPressed = false
                    onState(false)
                }
            }
    )
}

@Composable
fun ActionButtons(
    onButtonDown: (Int) -> Unit,
    onButtonUp: (Int) -> Unit
) {
    val padSize = 140.dp
    val buttonSize = 44.dp
    
    Box(
        modifier = Modifier
            .size(padSize)
            .background(Color(0xFF1E1C1B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF2C2A29),
                radius = size.minDimension / 2f,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Y Button (Top)
        GamepadButton(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 12.dp),
            text = "Y",
            size = buttonSize,
            onDown = { onButtonDown(0x08) },
            onUp = { onButtonUp(0x08) }
        )
        // X Button (Left)
        GamepadButton(
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 12.dp),
            text = "X",
            size = buttonSize,
            onDown = { onButtonDown(0x04) },
            onUp = { onButtonUp(0x04) }
        )
        // B Button (Right)
        GamepadButton(
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = -12.dp),
            text = "B",
            size = buttonSize,
            onDown = { onButtonDown(0x02) },
            onUp = { onButtonUp(0x02) }
        )
        // A Button (Bottom)
        GamepadButton(
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = -12.dp),
            text = "A",
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
    size: Dp,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    Box(
        modifier = modifier
            .size(size)
            .background(if (isPressed) Color(0xFF666463) else Color(0xFF383534), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
    }
}
