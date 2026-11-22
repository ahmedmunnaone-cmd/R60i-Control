package com.oppzippy.openscq30.ui.devicesettings.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oppzippy.openscq30.lib.bindings.translateSettingId
import com.oppzippy.openscq30.lib.wrapper.Range
import com.oppzippy.openscq30.lib.wrapper.Setting
import com.oppzippy.openscq30.lib.wrapper.Value
import com.oppzippy.openscq30.lib.wrapper.toValue
import com.oppzippy.openscq30.ui.theme.amberWarn
import com.oppzippy.openscq30.ui.theme.limeGreen
import com.oppzippy.openscq30.ui.theme.neonGreen
import com.oppzippy.openscq30.ui.theme.redWarn
import com.oppzippy.openscq30.ui.utils.LabeledSwitch
import com.oppzippy.openscq30.ui.utils.OptionalSelect
import com.oppzippy.openscq30.ui.utils.Select
import com.oppzippy.openscq30.ui.utils.throttledState
import kotlin.math.roundToInt

private val SOUND_MODE_IDS = setOf(
    "ambientSoundMode",
    "transparencyMode",
    "noiseCancelingMode",
    "adaptiveNoiseCanceling",
    "manualNoiseCanceling",
    "multiSceneNoiseCanceling",
    "windNoiseSuppression",
    "windNoiseDetected",
    "realTimeAdaptiveNoiseCanceling",
)
private val BATTERY_IDS = setOf(
    "batteryLevelLeft",
    "batteryLevelRight",
    "isChargingLeft",
    "isChargingRight",
    "caseBatteryLevel",
)
private val EQ_IDS = setOf("presetEqualizerProfile", "customEqualizerProfile", "volumeAdjustments")

private const val PREFS_NAME = "r60i_control"
private const val PREF_AUTO_BASS = "auto_bass_on_connect"

/** Remembers which connection already got the automatic Bass+ profile so we don't override manual changes. */
private object AutoBassState {
    @Volatile
    var lastConnection: Any? = null
}

@Composable
fun HomeDashboardScreen(
    connectionKey: Any,
    modelName: String,
    settings: List<Pair<String, Setting>>,
    setSettings: (List<Pair<String, Value>>) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQuickPresets: () -> Unit,
    onOpenAllSettings: () -> Unit,
) {
    val byId = settings.toMap()
    val set: (String, Value) -> Unit = { id, value -> setSettings(listOf(id to value)) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var autoBass by remember { mutableStateOf(prefs.getBoolean(PREF_AUTO_BASS, true)) }

    val volumeSetting = byId["volumeAdjustments"] as? Setting.EqualizerSetting
    LaunchedEffect(volumeSetting != null, autoBass) {
        if (autoBass && volumeSetting != null && AutoBassState.lastConnection !== connectionKey) {
            AutoBassState.lastConnection = connectionKey
            set("volumeAdjustments", bassPlusProfile(volumeSetting).toValue())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Reveal(0) { HeroHeader(modelName, byId) }
        Reveal(1) { ListeningModeCard(byId, set) }
        Reveal(2) {
            EqualizerCard(
                byId = byId,
                set = set,
                autoBass = autoBass,
                onAutoBassChange = {
                    autoBass = it
                    prefs.edit().putBoolean(PREF_AUTO_BASS, it).apply()
                },
                onOpenEqualizer = onOpenEqualizer,
            )
        }
        Reveal(3) { QuickTogglesCard(settings, set) }
        Reveal(4) { ControlsCard(settings, set) }
        Reveal(5) { GesturesCard(settings, set) }
        Reveal(6) { ActionsAndInfoCard(settings, set) }
        Reveal(7) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onOpenQuickPresets) { Text("Quick presets") }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onOpenAllSettings) { Text("All settings") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ───────────────────────────── building blocks ─────────────────────────────

@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(450, delayMillis = index * 80)) +
            slideInVertically(tween(450, delayMillis = index * 80)) { it / 5 },
    ) {
        content()
    }
}

@Composable
internal fun GlassCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0F1A14), Color(0xFF070B09))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(neonGreen.copy(alpha = 0.45f), Color.Transparent, neonGreen.copy(alpha = 0.15f))),
                shape,
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = neonGreen,
            )
        }
        content()
    }
}

@Composable
internal fun ChipRow(
    title: String?,
    options: List<String>,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                val bg by animateColorAsState(
                    if (selected) neonGreen else Color(0xFF121C16),
                    tween(250),
                    label = "chipBg",
                )
                val fg by animateColorAsState(
                    if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
                    tween(250),
                    label = "chipFg",
                )
                val scale by animateFloatAsState(
                    if (selected) 1.06f else 1f,
                    spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                    label = "chipScale",
                )
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(50))
                        .background(bg)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = labels.getOrElse(index) { option },
                        color = fg,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeControl(name: String, range: Range<Int>, value: Int, onChange: (Int) -> Unit) {
    val (shown, setShown) = throttledState(value, 250L) { onChange(it) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name)
            Text(shown.toString(), color = neonGreen, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = shown.toFloat(),
            onValueChange = { setShown(it.roundToInt()) },
            valueRange = range.start.toFloat()..range.end.toFloat(),
            steps = ((range.end - range.start) / range.step - 1).coerceAtLeast(0),
        )
    }
}

@Composable
private fun ToggleTile(name: String, checked: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val bg by animateColorAsState(if (checked) Color(0xFF0B3D22) else Color(0xFF0E1511), tween(300), label = "tileBg")
    val borderColor by animateColorAsState(
        if (checked) neonGreen else Color(0xFF1E2E25),
        tween(300),
        label = "tileBorder",
    )
    val glow by animateFloatAsState(if (checked) 1f else 0f, tween(400), label = "tileGlow")
    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable { onChange(!checked) }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(neonGreen.copy(alpha = 0.25f + 0.75f * glow)),
        )
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
        Text(
            text = if (checked) "ON" else "OFF",
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) neonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ───────────────────────────── hero + battery ─────────────────────────────

@Composable
private fun HeroHeader(modelName: String, byId: Map<String, Setting>) {
    GlassCard {
        val transition = rememberInfiniteTransition(label = "hero")
        val pulse by transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse",
        )
        val sweep by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
            label = "sweep",
        )
        val dot by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "dot",
        )
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2.6f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(neonGreen.copy(alpha = 0.35f), Color.Transparent),
                        center = c,
                        radius = r * 1.7f * pulse,
                    ),
                    radius = r * 1.7f * pulse,
                    center = c,
                )
                drawCircle(
                    color = neonGreen.copy(alpha = 0.25f),
                    radius = r * pulse,
                    center = c,
                    style = Stroke(2.dp.toPx()),
                )
                rotate(sweep, pivot = c) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.28f to neonGreen,
                            1f to Color.Transparent,
                            center = c,
                        ),
                        startAngle = 0f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(neonGreen.copy(alpha = dot)),
                    )
                    Text("Connected", color = neonGreen, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            BatteryRing("Left", byId["batteryLevelLeft"], byId["isChargingLeft"])
            BatteryRing("Case", byId["caseBatteryLevel"], null)
            BatteryRing("Right", byId["batteryLevelRight"], byId["isChargingRight"])
        }
    }
}

private fun batteryFraction(setting: Setting?): Float? {
    val raw = (setting as? Setting.InformationSetting)?.value ?: return null
    val parts = raw.split("/")
    if (parts.size != 2) return null
    val a = parts[0].trim().toFloatOrNull() ?: return null
    val b = parts[1].trim().toFloatOrNull() ?: return null
    if (b <= 0f) return null
    return (a / b).coerceIn(0f, 1f)
}

@Composable
private fun BatteryRing(label: String, level: Setting?, charging: Setting?) {
    val fraction = batteryFraction(level)
    val percentText = (level as? Setting.InformationSetting)?.translatedValue ?: "—"
    val isCharging = (charging as? Setting.InformationSetting)?.value
        ?.let { it.equals("true", true) || it.equals("yes", true) } ?: false

    val animated by animateFloatAsState(fraction ?: 0f, tween(1200, easing = FastOutSlowInEasing), label = "battery")
    val color by animateColorAsState(
        when {
            fraction == null -> Color(0xFF4F6A5B)
            fraction < 0.2f -> redWarn
            fraction < 0.45f -> amberWarn
            else -> neonGreen
        },
        tween(600),
        label = "batteryColor",
    )
    val transition = rememberInfiniteTransition(label = "charge")
    val chargeAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "chargeAlpha",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 7.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke),
                )
                drawArc(
                    color = color.copy(alpha = if (isCharging) chargeAlpha else 1f),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Text(percentText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isCharging) "$label ⚡" else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ───────────────────────────── listening mode ─────────────────────────────

@Composable
private fun ModeSelector(options: List<String>, labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFF0E1511))
            .border(1.dp, Color(0xFF1E2E25), RoundedCornerShape(30.dp)),
    ) {
        val segment = maxWidth / options.size.coerceAtLeast(1)
        val offset by animateDpAsState(
            segment * selected.coerceAtLeast(0),
            spring(dampingRatio = 0.7f, stiffness = 300f),
            label = "segment",
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.horizontalGradient(listOf(neonGreen, limeGreen))),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val fg by animateColorAsState(
                    if (index == selected) Color.Black else MaterialTheme.colorScheme.onSurface,
                    tween(250),
                    label = "modeFg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labels.getOrElse(index) { option },
                        color = fg,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningModeCard(byId: Map<String, Setting>, set: (String, Value) -> Unit) {
    val mode = byId["ambientSoundMode"] as? Setting.SelectSetting ?: return
    GlassCard(title = translateSettingId("ambientSoundMode")) {
        ModeSelector(
            options = mode.setting.options,
            labels = mode.setting.localizedOptions,
            selected = mode.setting.options.indexOf(mode.value),
            onSelect = { set("ambientSoundMode", mode.setting.options[it].toValue()) },
        )
        AnimatedContent(
            targetState = mode.value,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label = "modeDetails",
        ) { current ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    current.equals("NoiseCanceling", true) -> NoiseCancelingControls(byId, set)
                    current.equals("Transparency", true) -> TransparencyControls(byId, set)
                    else -> Text(
                        "Normal mode – no noise cancelling or ambient pass-through.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectChips(id: String, setting: Setting?, set: (String, Value) -> Unit) {
    val select = setting as? Setting.SelectSetting ?: return
    ChipRow(
        title = translateSettingId(id),
        options = select.setting.options,
        labels = select.setting.localizedOptions,
        selectedIndex = select.setting.options.indexOf(select.value),
        onSelect = { set(id, select.setting.options[it].toValue()) },
    )
}

@Composable
private fun NoiseCancelingControls(byId: Map<String, Setting>, set: (String, Value) -> Unit) {
    val ncMode = (byId["noiseCancelingMode"] as? Setting.SelectSetting)?.value
    SelectChips("noiseCancelingMode", byId["noiseCancelingMode"], set)

    val manual = byId["manualNoiseCanceling"] as? Setting.I32RangeSetting
    if (manual != null && (ncMode == null || ncMode.equals("Manual", true))) {
        RangeControl(
            name = translateSettingId("manualNoiseCanceling"),
            range = manual.setting,
            value = manual.value,
            onChange = { set("manualNoiseCanceling", it.toValue()) },
        )
    }
    val adaptive = byId["adaptiveNoiseCanceling"] as? Setting.InformationSetting
    if (adaptive != null && (ncMode == null || ncMode.equals("Adaptive", true))) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(translateSettingId("adaptiveNoiseCanceling"))
            Text(adaptive.translatedValue, color = neonGreen, fontWeight = FontWeight.Bold)
        }
    }
    if (ncMode == null || ncMode.equals("MultiScene", true)) {
        SelectChips("multiSceneNoiseCanceling", byId["multiSceneNoiseCanceling"], set)
    }
    ToggleRow("windNoiseSuppression", byId["windNoiseSuppression"], set)
    ToggleRow("realTimeAdaptiveNoiseCanceling", byId["realTimeAdaptiveNoiseCanceling"], set)
}

@Composable
private fun TransparencyControls(byId: Map<String, Setting>, set: (String, Value) -> Unit) {
    SelectChips("transparencyMode", byId["transparencyMode"], set)
    ToggleRow("windNoiseSuppression", byId["windNoiseSuppression"], set)
}

@Composable
private fun ToggleRow(id: String, setting: Setting?, set: (String, Value) -> Unit) {
    val toggle = setting as? Setting.ToggleSetting ?: return
    LabeledSwitch(
        label = translateSettingId(id),
        isChecked = toggle.value,
        onCheckedChange = { set(id, it.toValue()) },
    )
}

// ───────────────────────────── equalizer card ─────────────────────────────

@Composable
private fun EqualizerCard(
    byId: Map<String, Setting>,
    set: (String, Value) -> Unit,
    autoBass: Boolean,
    onAutoBassChange: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    val volume = byId["volumeAdjustments"] as? Setting.EqualizerSetting
    val preset = byId["presetEqualizerProfile"] as? Setting.PresetEqualizerProfileSelect
    if (volume == null && preset == null) return
    GlassCard(title = "Equalizer") {
        if (volume != null) {
            AnimatedEqualizerCurve(volume, Modifier.fillMaxWidth().height(110.dp))
        }
        if (preset != null) {
            ChipRow(
                title = null,
                options = preset.select.options,
                labels = preset.select.localizedOptions,
                selectedIndex = preset.select.options.indexOf(preset.value),
                onSelect = { set("presetEqualizerProfile", preset.select.options[it].toValue()) },
            )
        }
        if (volume != null) {
            BassBoostControl(volume) { set("volumeAdjustments", it.toValue()) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = neonGreen, contentColor = Color.Black),
                    onClick = { set("volumeAdjustments", bassPlusProfile(volume).toValue()) },
                ) { Text("Bass+ preset", fontWeight = FontWeight.Bold) }
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onOpenEqualizer) { Text("EQ Studio") }
            }
            LabeledSwitch(
                label = "Apply Bass+ when earbuds connect",
                isChecked = autoBass,
                onCheckedChange = onAutoBassChange,
            )
        } else {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenEqualizer) { Text("EQ Studio") }
        }
    }
}

// ───────────────────────────── generic controls ─────────────────────────────

private fun otherSettings(settings: List<Pair<String, Setting>>) =
    settings.filter { (id, _) -> id !in SOUND_MODE_IDS && id !in BATTERY_IDS && id !in EQ_IDS }

@Composable
private fun QuickTogglesCard(settings: List<Pair<String, Setting>>, set: (String, Value) -> Unit) {
    val toggles = otherSettings(settings).filter { it.second is Setting.ToggleSetting }
    if (toggles.isEmpty()) return
    GlassCard(title = "Quick toggles") {
        toggles.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (id, setting) ->
                    ToggleTile(
                        name = translateSettingId(id),
                        checked = (setting as Setting.ToggleSetting).value,
                        modifier = Modifier.weight(1f),
                        onChange = { set(id, it.toValue()) },
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ControlsCard(settings: List<Pair<String, Setting>>, set: (String, Value) -> Unit) {
    val items = otherSettings(settings).filter { it.second is Setting.SelectSetting || it.second is Setting.I32RangeSetting }
    if (items.isEmpty()) return
    GlassCard(title = "Controls") {
        items.forEach { (id, setting) ->
            when (setting) {
                is Setting.I32RangeSetting -> RangeControl(
                    name = translateSettingId(id),
                    range = setting.setting,
                    value = setting.value,
                    onChange = { set(id, it.toValue()) },
                )

                is Setting.SelectSetting -> Select(
                    name = translateSettingId(id),
                    options = setting.setting.localizedOptions,
                    selectedIndex = setting.setting.options.indexOf(setting.value),
                    onSelect = { set(id, setting.setting.options[it].toValue()) },
                )

                else -> {}
            }
        }
    }
}

@Composable
private fun GesturesCard(settings: List<Pair<String, Setting>>, set: (String, Value) -> Unit) {
    val gestures = otherSettings(settings).filter { it.second is Setting.OptionalSelectSetting }
    if (gestures.isEmpty()) return
    GlassCard(title = "Touch gestures") {
        gestures.forEach { (id, setting) ->
            val optional = setting as Setting.OptionalSelectSetting
            OptionalSelect(
                name = translateSettingId(id),
                options = optional.setting.localizedOptions,
                selectedIndex = optional.value?.let { optional.setting.options.indexOf(it) },
                onSelect = { index -> set(id, index?.let { optional.setting.options[it] }.toValue()) },
            )
        }
    }
}

@Composable
private fun ActionsAndInfoCard(settings: List<Pair<String, Setting>>, set: (String, Value) -> Unit) {
    val others = otherSettings(settings)
    val actions = others.filter { it.second is Setting.Action }
    val info = others.filter { it.second is Setting.InformationSetting }
    if (actions.isEmpty() && info.isEmpty()) return
    GlassCard(title = "Device") {
        info.forEach { (id, setting) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(translateSettingId(id), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    (setting as Setting.InformationSetting).translatedValue,
                    modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
        actions.forEach { (id, _) ->
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { set(id, true.toValue()) }) {
                Text(translateSettingId(id))
            }
        }
    }
}
