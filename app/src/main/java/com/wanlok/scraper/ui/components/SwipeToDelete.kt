package com.wanlok.scraper.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DeleteButtonWidth = 96.dp

// A flick faster than this opens or closes the row, however far it was pulled.
private val FlingSpeed = 300.dp

/**
 * Swipe the [content] to the left to reveal a Delete button. The row stays open until Delete is
 * tapped, the row is tapped, or it is swiped back.
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val buttonWidthPx = with(density) { DeleteButtonWidth.toPx() }
    val flingSpeedPx = with(density) { FlingSpeed.toPx() }

    // How far the row has moved: 0 when closed, -buttonWidthPx when open. It is plain state,
    // changed straight from the drag, so the row follows the finger with no delay.
    var offset by remember { mutableFloatStateOf(0f) }
    var animation by remember { mutableStateOf<Job?>(null) }
    val open by remember { derivedStateOf { offset < 0f } }

    fun settle(target: Float, velocity: Float = 0f) {
        animation?.cancel()
        animation = scope.launch {
            animate(offset, target, velocity) { value, _ -> offset = value }
        }
    }

    Box(modifier = modifier) {
        // The button sits behind the row and is uncovered as the row moves left.
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .width(DeleteButtonWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Delete", color = MaterialTheme.colorScheme.onError)
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                // Opaque, so the button stays hidden until the row is moved.
                .background(MaterialTheme.colorScheme.background)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offset = (offset + delta).coerceIn(-buttonWidthPx, 0f)
                    },
                    // A new drag takes over from a row that is still settling.
                    onDragStarted = { animation?.cancel() },
                    // Settle open or closed: by the flick if there was one, else by how far
                    // the row was pulled.
                    onDragStopped = { velocity ->
                        val opens = when {
                            velocity < -flingSpeedPx -> true
                            velocity > flingSpeedPx -> false
                            else -> offset < -buttonWidthPx / 2
                        }
                        settle(if (opens) -buttonWidthPx else 0f, velocity)
                    }
                )
        ) {
            content()
            if (open) {
                // While open, a tap on the row closes it instead of opening the row.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { settle(0f) }
                )
            }
        }
    }
}
