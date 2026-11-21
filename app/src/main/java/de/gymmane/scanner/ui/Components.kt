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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import de.gymmane.scanner.ui.theme.*

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(text.uppercase(java.util.Locale.GERMAN), modifier, color = color, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun CopyField(label: String, value: String?, onCopy: (String, String) -> Unit, modifier: Modifier = Modifier, color: Color = Ink, unit: String? = null, large: Boolean = false) {
    Surface(modifier = modifier, color = Field, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(label, Modifier.weight(1f), color = color)
                IconButton(onClick = { value?.let { onCopy(label, it) } }, enabled = value != null, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.ContentCopy, T.format("%1\$s kopieren", label), Modifier.size(16.dp), tint = if (value != null) Muted else Muted.copy(alpha = .3f))
                }
            }
            Crossfade(targetState = value, animationSpec = Motion.spec(), label = "nutritionValue") { displayed ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(displayed ?: "—", style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                        color = if (displayed == null) Muted else Ink, maxLines = 1)
                    if (displayed != null && unit != null) {
                        Spacer(Modifier.width(4.dp))
                        Text(unit, color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = if (large) 5.dp else 3.dp))
                    }
                }
            }
            if (value == null) Text(T.text("Nicht angegeben"), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, numeric: Boolean = false,
    unit: String? = null, accent: Color = Muted, enabled: Boolean = true, isError: Boolean = false) {
    TextField(value, onValueChange, modifier.fillMaxWidth(), enabled = enabled, singleLine = true, isError = isError,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(label, color = if (isError) MaterialTheme.colorScheme.error else accent) },
        suffix = unit?.let { { Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(focusedContainerColor = Field, unfocusedContainerColor = Field, disabledContainerColor = Field, errorContainerColor = Field,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent,
            cursorColor = Apricot))
}

@Composable
fun ExpandRow(text: String, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val turn by animateFloatAsState(if (expanded) 180f else 0f, Motion.spec(Motion.Layout), label = "expandArrow")
    val interactions = remember { MutableInteractionSource() }
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth().motionPress(interactions), color = Surface, shape = RoundedCornerShape(50), interactionSource = interactions) {
        Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Muted, modifier = Modifier.size(20.dp).rotate(turn))
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
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).background(Field, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Apricot, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
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
            Text(it, style = MaterialTheme.typography.labelLarge)
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

@Composable
fun DialogConfirm(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick, enabled = enabled, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = Modifier.heightIn(min = 44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Background, disabledContainerColor = Field, disabledContentColor = Muted)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DialogDismiss(text: String, onClick: () -> Unit) {
    TextButton(onClick, modifier = Modifier.heightIn(min = 44.dp), colors = ButtonDefaults.textButtonColors(contentColor = Muted)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
