package de.gymmane.scanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import de.gymmane.scanner.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import de.gymmane.scanner.i18n.T
import de.gymmane.scanner.ui.theme.*

@Composable
fun LanguageChoices(language: String, onLanguage: (String) -> Unit) {
    SettingsCard {
        listOf("de" to "Deutsch", "en" to "English").forEachIndexed { index, (code, label) ->
            if (index > 0) SettingsDivider()
            val interaction = remember(code) { MutableInteractionSource() }
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).semantics { selected = language == code }.clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple()) { onLanguage(code) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Canvas(Modifier.size(32.dp, 22.dp).clip(RoundedCornerShape(5.dp))) {
                    if (code == "de") {
                        listOf(Color(0xFF353533), Color(0xFFB96858), Color(0xFFD5B56C)).forEachIndexed { index, color ->
                            drawRect(color, Offset(0f, size.height * index / 3), Size(size.width, size.height / 3))
                        }
                    } else {
                        drawRect(Color(0xFF384B69))
                        drawLine(Ink, Offset.Zero, Offset(size.width, size.height), size.height * .24f)
                        drawLine(Ink, Offset(0f, size.height), Offset(size.width, 0f), size.height * .24f)
                        drawLine(Color(0xFFB96858), Offset.Zero, Offset(size.width, size.height), size.height * .09f)
                        drawLine(Color(0xFFB96858), Offset(0f, size.height), Offset(size.width, 0f), size.height * .09f)
                        drawLine(Ink, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), size.width * .25f)
                        drawLine(Ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height * .36f)
                        drawLine(Color(0xFFB96858), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), size.width * .13f)
                        drawLine(Color(0xFFB96858), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height * .19f)
                    }
                }
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                val check by animateColorAsState(if (language == code) Apricot else Color.Transparent, Motion.spec(), label = "languageCheck")
                Icon(Icons.Outlined.Check, null, tint = check, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun OnboardingScreen(language: String, onLanguage: (String) -> Unit, onContinue: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Background, contentColor = Ink) {
    ScreenEntrance("onboarding") {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
            Box(Modifier.size(60.dp).background(Surface, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.QrCodeScanner, null, tint = Apricot, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("GymMane Scanner".uppercase(java.util.Locale.ROOT), color = Apricot, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Text(T.text("Scannen. Prüfen. Teilen."), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))
            Text(T.text("Finde Nährwerte per Barcode und teile das Lebensmittel direkt mit GymMane."), color = Muted, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Surface(color = Surface, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    OnboardingStep(Icons.Outlined.QrCodeScanner, T.text("Barcode scannen"))
                    OnboardingDivider()
                    OnboardingStep(Icons.AutoMirrored.Outlined.FactCheck, T.text("Werte prüfen"))
                    OnboardingDivider()
                    OnboardingStep(Icons.Outlined.IosShare, T.text("Mit GymMane teilen"), T.text("Nach dem Scan öffnet sich der Teilen-Dialog. Wähle GymMane zum Einfügen."))
                }
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel(T.text("Sprache"), Modifier.padding(start = 4.dp, bottom = 10.dp))
            LanguageChoices(language, onLanguage)
        }
        PrimaryButton(T.text("Loslegen"), onContinue, Modifier.fillMaxWidth().padding(top = 16.dp), icon = Icons.AutoMirrored.Outlined.ArrowForward)
    }
    }
    }
}

@Composable
private fun OnboardingStep(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Apricot, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun OnboardingDivider() {
    HorizontalDivider(Modifier.padding(start = 58.dp, end = 16.dp), color = Outline.copy(alpha = .6f))
}

@Composable
fun SettingsScreen(language: String, autoSend: Boolean, onLanguage: (String) -> Unit, onAutoSend: (Boolean) -> Unit,
    onIntro: () -> Unit, onRepository: () -> Unit, onLicense: () -> Unit, updates: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LanguageChoices(language, onLanguage)
        SettingsCard {
            SettingsRow(Icons.AutoMirrored.Outlined.Send, T.text("Automatisch an GymMane senden nach Scan"),
                T.text("Nach dem Scan öffnet sich der Teilen-Dialog. Wähle GymMane zum Einfügen."), onClick = { onAutoSend(!autoSend) }) {
                Switch(autoSend, onAutoSend, modifier = Modifier.semantics { contentDescription = T.text("Automatisch an GymMane senden nach Scan") })
            }
        }
        updates()
        SettingsCard {
            SettingsRow(Icons.Outlined.Info, T.text("App-Version")) {
                Text(BuildConfig.VERSION_NAME, color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            SettingsDivider()
            SettingsRow(Icons.Outlined.Code, T.text("Quellcode"),
                if (BuildConfig.REPOSITORY_URL.isBlank()) T.text("Repository-Link noch nicht hinterlegt") else null,
                onClick = if (BuildConfig.REPOSITORY_URL.isNotBlank()) onRepository else null) {
                if (BuildConfig.REPOSITORY_URL.isNotBlank()) Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = Muted, modifier = Modifier.size(18.dp))
            }
            SettingsDivider()
            SettingsRow(Icons.Outlined.Replay, T.text("Onboarding erneut ansehen"), onClick = onIntro) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(20.dp))
            }
            SettingsDivider()
            SettingsRow(Icons.Outlined.Public, "Open Food Facts", T.text("Produktdaten: Open Food Facts · ODbL. Bilder: CC BY-SA."), onClick = onLicense) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = Muted, modifier = Modifier.size(18.dp))
            }
        }
        Text(T.text("Barcodes werden bei Open Food Facts gesucht. Verlauf und manuelle Einträge bleiben auf deinem Gerät. Kamerabilder werden nicht hochgeladen."),
            color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Surface, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), content = content)
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(start = 58.dp, end = 16.dp), color = Outline.copy(alpha = .6f))
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    Row((if (onClick != null) base.clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick) else base)
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Apricot, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}
