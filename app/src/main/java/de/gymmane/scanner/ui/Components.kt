package de.gymmane.scanner.ui

import de.gymmane.scanner.i18n.T

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gymmane.scanner.ui.theme.*

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(text.uppercase(java.util.Locale.GERMAN), modifier, color = color, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun CopyField(label: String, value: String?, onCopy: (String, String) -> Unit, modifier: Modifier = Modifier, color: Color = Ink, unit: String? = null, large: Boolean = false) {
    Surface(modifier = modifier, color = Field, shape = RoundedCornerShape(23.dp)) {
        Column(Modifier.padding(start = 17.dp, end = 8.dp, top = 14.dp, bottom = 10.dp)) {
            SectionLabel(label, color = color)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Crossfade(targetState = value, animationSpec = Motion.spec(), label = "nutritionValue") { displayed ->
                        Text(displayed ?: "—", style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            color = if (displayed == null) Muted else Ink)
                    }
                    if (value == null) Text(T.text("Nicht angegeben"), color = Muted, fontSize = 11.sp)
                    else if (unit != null) Text(unit, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { value?.let { onCopy(label, it) } }, enabled = value != null) {
                    Icon(Icons.Outlined.ContentCopy, T.format("%1\$s kopieren", label), Modifier.size(18.dp), tint = if (value != null) color else Muted.copy(alpha = .3f))
                }
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Surface(modifier, color = Surface, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Apricot.copy(alpha = .4f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = Apricot, modifier = Modifier.size(20.dp))
                Crossfade(T.text(message), modifier = Modifier.weight(1f), animationSpec = Motion.spec(), label = "errorMessage") {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
        }
    }
}

@Composable
fun MotionError(message: String?, visibility: androidx.compose.animation.core.MutableTransitionState<Boolean> = rememberMotionVisibility(message != null), resize: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    var retained by remember { mutableStateOf("") }
    if (message != null) SideEffect { retained = message }
    AnimatedVisibility(visibility,
        enter = if (resize) Motion.enter() else androidx.compose.animation.fadeIn(Motion.spec()),
        exit = if (resize) Motion.exit() else androidx.compose.animation.fadeOut(Motion.spec(Motion.Exit, true))) {
        ErrorCard(message ?: retained, actions = actions)
    }
}

@Composable
fun EmptyCard(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: @Composable () -> Unit) {
    Surface(modifier = modifier, color = Surface, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = Apricot, modifier = Modifier.size(36.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium)
            action()
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val interactions = remember { MutableInteractionSource() }
    Button(onClick, modifier.heightIn(min = 56.dp).motionPress(interactions, enabled), enabled = enabled,
        interactionSource = interactions,
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Background), shape = RoundedCornerShape(50)) {
        if (icon != null) { Icon(icon, null, Modifier.size(19.dp)); Spacer(Modifier.width(10.dp)) }
        Crossfade(text, animationSpec = Motion.spec(), label = "buttonLabel") {
            Text(it, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PillSwitch(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, enabled: (Int) -> Boolean = { true }) {
    Row(modifier.background(Field, RoundedCornerShape(50)).padding(4.dp)) {
        labels.forEachIndexed { index, label ->
            val background by animateColorAsState(if (selected == index) Ink else Color.Transparent, Motion.spec(), label = "selection")
            TextButton(onClick = { onSelect(index) }, enabled = enabled(index),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { this.selected = selected == index }, shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = background,
                    contentColor = if (selected == index) Background else Muted,
                    disabledContainerColor = Color.Transparent, disabledContentColor = Muted.copy(alpha = .45f))) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
