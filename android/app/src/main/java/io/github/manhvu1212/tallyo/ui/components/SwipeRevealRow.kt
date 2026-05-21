package io.github.manhvu1212.tallyo.ui.components

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.manhvu1212.tallyo.ui.theme.TallyoColors
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class SwipeAction(
    val icon: ImageVector,
    val contentDescription: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

internal enum class SwipeRevealValue { Closed, Open }

private val ActionPillSize = 48.dp
private val ActionPillGap = 12.dp
private val ActionSlide = ActionPillSize + ActionPillGap * 2  // 72.dp total

/**
 * Coordinates a single open swipe across multiple [SwipeRevealRow]s on the same screen.
 * Wrap the screen with [SwipeRevealHostScope] and the rows pick up the host via composition local.
 */
@OptIn(ExperimentalFoundationApi::class)
class SwipeRevealHost internal constructor(private val scope: CoroutineScope) {
    internal var openState: AnchoredDraggableState<SwipeRevealValue>? = null
        private set
    internal var openBoundsInRoot: Rect? = null
        private set

    internal fun register(state: AnchoredDraggableState<SwipeRevealValue>, bounds: Rect) {
        val previous = openState
        if (previous != null && previous !== state) {
            scope.launch { previous.animateTo(SwipeRevealValue.Closed) }
        }
        openState = state
        openBoundsInRoot = bounds
    }

    internal fun unregister(state: AnchoredDraggableState<SwipeRevealValue>) {
        if (openState === state) {
            openState = null
            openBoundsInRoot = null
        }
    }

    /** Returns true if a row was closed because the tap landed outside its bounds. */
    internal fun closeIfOutside(pointerInRoot: Offset): Boolean {
        val state = openState ?: return false
        val bounds = openBoundsInRoot ?: return false
        if (!bounds.contains(pointerInRoot)) {
            scope.launch { state.animateTo(SwipeRevealValue.Closed) }
            return true
        }
        return false
    }
}

internal val LocalSwipeRevealHost = compositionLocalOf<SwipeRevealHost?> { null }

/**
 * Wraps content with a [SwipeRevealHost]. Taps outside the currently open row close it and are
 * consumed, so the underlying element does not fire.
 */
@Composable
fun SwipeRevealHostScope(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val host = remember(scope) { SwipeRevealHost(scope) }
    CompositionLocalProvider(LocalSwipeRevealHost provides host) {
        Box(
            modifier = modifier.pointerInput(host) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.firstOrNull { it.changedToDown() }?.let { change ->
                            if (host.closeIfOutside(change.position)) {
                                change.consume()
                            }
                        }
                    }
                }
            },
        ) {
            content()
        }
    }
}

/**
 * Swipe-left to reveal a circular icon button; user taps the icon to trigger.
 * Tapping the content while open dismisses the swipe. With a [SwipeRevealHostScope] ancestor,
 * tapping outside the row also dismisses it.
 * Wrapped content must be opaque so the action layer is hidden when closed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeRevealRow(
    action: SwipeAction,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val slidePx = with(density) { ActionSlide.toPx() }
    val host = LocalSwipeRevealHost.current

    val state = remember {
        AnchoredDraggableState(
            initialValue = SwipeRevealValue.Closed,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay(),
        )
    }
    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                SwipeRevealValue.Closed at 0f
                SwipeRevealValue.Open at -slidePx
            },
        )
    }

    var bounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(state.currentValue, bounds, host) {
        val h = host ?: return@LaunchedEffect
        val b = bounds
        if (state.currentValue == SwipeRevealValue.Open && b != null) {
            h.register(state, b)
        } else {
            h.unregister(state)
        }
    }
    DisposableEffect(host) {
        onDispose { host?.unregister(state) }
    }

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            bounds = coords.boundsInRoot()
        },
    ) {
        // Action pill (sits behind, revealed when content slides left)
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val bg = if (action.destructive) TallyoColors.Danger else TallyoColors.Primary
            Box(
                modifier = Modifier
                    .padding(end = ActionPillGap)
                    .size(ActionPillSize)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable {
                        action.onClick()
                        scope.launch { state.animateTo(SwipeRevealValue.Closed) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    action.icon,
                    contentDescription = action.contentDescription,
                    tint = Color.White,
                )
            }
        }
        // Foreground content (slides horizontally)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state, orientation = Orientation.Horizontal)
                .pointerInput(state.currentValue) {
                    if (state.currentValue == SwipeRevealValue.Open) {
                        detectTapGestures(
                            onTap = {
                                scope.launch { state.animateTo(SwipeRevealValue.Closed) }
                            },
                        )
                    }
                },
        ) {
            content()
        }
    }
}
