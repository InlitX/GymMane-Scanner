package de.gymmane.scanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
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
import de.gymmane.scanner.i18n.T
import de.gymmane.scanner.ui.theme.*

@Composable
fun LanguageChoices(language: String, onLanguage: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("de" to "Deutsch", "en" to "English").forEach { (code, label) ->
            val background by animateColorAsState(if (language == code) Field else Surface, Motion.spec(), label = "language")
            val interaction = remember(code) { MutableInteractionSource() }
            Surface(color = background, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().semantics { selected = language == code }.motionPress(interaction).clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple()) { onLanguage(code) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Canvas(Modifier.size(36.dp, 24.dp).clip(RoundedCornerShape(5.dp))) {
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
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (language == code) Icon(Icons.Outlined.Check, null, tint = Apricot)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(language: String, onLanguage: (String) -> Unit, onContinue: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Background, contentColor = Ink) {
    ScreenEntrance("onboarding") {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("GymMane Scanner", color = Apricot, style = MaterialTheme.typography.titleMedium)
            Text(T.text("Scannen. Prüfen. Teilen."), style = MaterialTheme.typography.headlineLarge)
            Text(T.text("Finde Nährwerte per Barcode und teile das Lebensmittel direkt mit GymMane."), color = Muted)
            SectionLabel(T.text("Sprache"))
            LanguageChoices(language, onLanguage)
            Text(T.text("Nach dem Scan öffnet sich der Teilen-Dialog. Wähle GymMane zum Einfügen."), color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(T.text("Loslegen"), onContinue, Modifier.fillMaxWidth().padding(top = 20.dp))
    }
    }
    }
}

@Composable
fun SettingsScreen(language: String, autoSend: Boolean, onLanguage: (String) -> Unit, onAutoSend: (Boolean) -> Unit,
    onIntro: () -> Unit, onRepository: () -> Unit, onLicense: () -> Unit, updates: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SectionLabel(T.text("Sprache"))
        LanguageChoices(language, onLanguage)
        HorizontalDivider(color = Outline)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(T.text("Automatisch an GymMane senden nach Scan"), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(autoSend, onAutoSend, modifier = Modifier.semantics { contentDescription = T.text("Automatisch an GymMane senden nach Scan") })
        }
        Text(T.text("Nach dem Scan öffnet sich der Teilen-Dialog. Wähle GymMane zum Einfügen."), color = Muted, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(color = Outline)
        Row { Text(T.text("App-Version"), Modifier.weight(1f)); Text(BuildConfig.VERSION_NAME, color = Muted) }
        updates()
        TextButton(onClick = onRepository, enabled = BuildConfig.REPOSITORY_URL.isNotBlank()) { Text(T.text("Quellcode")) }
        if (BuildConfig.REPOSITORY_URL.isBlank()) Text(T.text("Repository-Link noch nicht hinterlegt"), color = Muted, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onIntro) { Text(T.text("Onboarding erneut ansehen")) }
        SectionLabel(T.text("Datenschutz & Datenquelle"))
        Text(T.text("Barcodes werden bei Open Food Facts gesucht. Verlauf und manuelle Einträge bleiben auf deinem Gerät. Kamerabilder werden nicht hochgeladen."), color = Muted, style = MaterialTheme.typography.bodySmall)
        Text(T.text("Produktdaten: Open Food Facts · ODbL. Bilder: CC BY-SA."), color = Muted, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onLicense) { Text("Open Food Facts") }
    }
}
