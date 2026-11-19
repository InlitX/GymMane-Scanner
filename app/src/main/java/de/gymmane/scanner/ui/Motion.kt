package de.gymmane.scanner.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberMotionVisibility(visible: Boolean): MutableTransitionState<Boolean> =
    remember { MutableTransitionState(false) }.apply { targetState = visible }

/** All app-owned motion. Material 3 retains its native ripple/switch/snackbar motion. */
object Motion {
    const val Feedback = 150
    const val Enter = 200
    const val Exit = 150
    const val Layout = 220
    const val Screen = 280
    val EaseOut: Easing = CubicBezierEasing(.2f, 0f, 0f, 1f)
    val EaseIn: Easing = CubicBezierEasing(.4f, 0f, 1f, 1f)

    @Composable
    fun <T> spec(duration: Int = Enter, exiting: Boolean = false): TweenSpec<T> =
        tween(if (LocalReducedMotion.current) 0 else duration, easing = if (exiting) EaseIn else EaseOut)

    @Composable fun enter(): EnterTransition = fadeIn(spec()) + expandVertically(spec(Layout))
    @Composable fun exit(): ExitTransition = fadeOut(spec(Exit, true)) + shrinkVertically(spec(Layout, true))
}

/** One observer for the whole theme, including changes while the app is open. */
@Composable
fun MotionProvider(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    fun disabled() = runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) <= 0f }.getOrDefault(false)
    var reduced by remember(resolver) { mutableStateOf(disabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = disabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        reduced = disabled()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalReducedMotion provides reduced, content = content)
}

/** Single-tree navigation: the camera is never retained for an outgoing transition. */
@Composable
fun ScreenEntrance(key: Any, enabled: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    if (!enabled || LocalReducedMotion.current) {
        Box(Modifier.fillMaxSize(), content = content)
        return
    }
    val progress = remember(key) { Animatable(0f) }
    val spec = Motion.spec<Float>(Motion.Screen)
    val distance = with(LocalDensity.current) { 6.dp.toPx() }
    LaunchedEffect(progress) { progress.animateTo(1f, spec) }
    Box(Modifier.fillMaxSize().graphicsLayer {
        alpha = .75f + progress.value * .25f
        translationY = distance * (1f - progress.value)
    }, content = content)
}

fun Modifier.motionPress(interactions: MutableInteractionSource, enabled: Boolean = true): Modifier = composed {
    val pressed by interactions.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed && enabled) .98f else 1f, Motion.spec(Motion.Feedback), label = "press")
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** Retains only the small dialog payload while fading out; callbacks still run immediately. */
@Composable
fun <T : Any> MotionDialog(value: T?, content: @Composable (T, Modifier) -> Unit) {
    if (LocalReducedMotion.current) {
        if (value != null) content(value, Modifier)
        return
    }
    var retained by remember { mutableStateOf<T?>(null) }
    if (value != null) SideEffect { retained = value }
    val visible = value != null
    val state = remember { MutableTransitionState(false) }.apply { targetState = visible }
    val transition = rememberTransition(state, label = "dialog")
    val entering = Motion.spec<Float>()
    val exiting = Motion.spec<Float>(Motion.Exit, true)
    val alpha = transition.animateFloat(transitionSpec = { if (targetState) entering else exiting }, label = "dialogAlpha") { if (it) 1f else 0f }
    val displayed = value ?: retained
    if ((state.currentState || state.targetState) && displayed != null) {
        content(displayed, Modifier.graphicsLayer {
            this.alpha = alpha.value
            scaleX = .98f + .02f * alpha.value
            scaleY = scaleX
        }.pointerInput(visible) {
            if (!visible) awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        })
    }
}
