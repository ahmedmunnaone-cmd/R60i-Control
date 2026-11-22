package com.oppzippy.openscq30.ui.devicesettings.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oppzippy.openscq30.lib.bindings.translateSettingId
import com.oppzippy.openscq30.lib.wrapper.ModifiableSelectCommandInner
import com.oppzippy.openscq30.lib.wrapper.Setting
import com.oppzippy.openscq30.lib.wrapper.Value
import com.oppzippy.openscq30.lib.wrapper.toValue
import com.oppzippy.openscq30.ui.devicesettings.composables.Equalizer
import com.oppzippy.openscq30.ui.devicesettings.composables.ReadOnlyEqualizer
import com.oppzippy.openscq30.ui.theme.neonGreen
import com.oppzippy.openscq30.ui.utils.ModifiableSelect
import com.oppzippy.openscq30.ui.utils.throttledState
import java.util.Locale
import kotlin.math.roundToInt

private const val SETTING_PRESET = "presetEqualizerProfile"
private const val SETTING_CUSTOM = "customEqualizerProfile"
private const val SETTING_VOLUME = "volumeAdjustments"

// Band gains are expressed in tenths of a dB (the R60i NC range is -12.0 dB .. +13.4 dB).
// The earbuds expose 8 visible bands: roughly 100 Hz, 200 Hz, 400 Hz, 800 Hz, 1.6 kHz, 3.2 kHz, 6.4 kHz, 12.8 kHz.
internal data class BassProfile(val name: String, val gains: List<Int>)

internal val BASS_PROFILES = listOf(
    BassProfile("Flat", listOf(0, 0, 0, 0, 0, 0, 0, 0)),
    BassProfile("Bass+", listOf(70, 55, 30, 10, 0, 0, 0, 0)),
    BassProfile("Bass++", listOf(100, 80, 45, 15, 0, -5, -5, 0)),
    BassProfile("Deep Bass", listOf(110, 70, 20, 0, -10, 0, 0, 0)),
    BassProfile("Bass + Vocal", listOf(70, 50, 20, 0, 10, 25, 20, 0)),
    BassProfile("Bass + Sparkle", listOf(60, 45, 15, 0, 0, 20, 35, 30)),
)

// How much each of the lowest bands follows the Bass Boost slider (1.0 = full strength).
private val BASS_WEIGHTS = floatArrayOf(1f, 0.78f, 0.42f, 0.15f)
private const val BASS_PEAK_TENTHS_DB = 100 // slider at 100% => +10.0 dB at the lowest band

internal fun fitGains(gains: List<Int>, setting: Setting.EqualizerSetting): List<Short> {
    val min = setting.setting.min.toInt()
    val max = setting.setting.max.toInt()
    return List(setting.value.size) { index ->
        (gains.getOrElse(index) { 0 }).coerceIn(min, max).toShort()
    }
}

internal fun bassPlusProfile(setting: Setting.EqualizerSetting): List<Short> =
    fitGains(BASS_PROFILES.first { it.name == "Bass+" }.gains, setting)

private fun applyBass(setting: Setting.EqualizerSetting, percent: Int): List<Short> {
    val min = setting.setting.min.toInt()
    val max = setting.setting.max.toInt()
    return setting.value.mapIndexed { index, old ->
        if (index < BASS_WEIGHTS.size) {
            (BASS_PEAK_TENTHS_DB * (percent / 100f) * BASS_WEIGHTS[index]).roundToInt().coerceIn(min, max).toShort()
        } else {
            old
        }
    }
}

private fun currentBassPercent(setting: Setting.EqualizerSetting): Int {
    val first = setting.value.firstOrNull()?.toInt() ?: 0
    return (first * 100f / BASS_PEAK_TENTHS_DB).roundToInt().coerceIn(0, 100)
}

@Composable
internal fun AnimatedEqualizerCurve(setting: Setting.EqualizerSetting, modifier: Modifier) {
    val animated = setting.value.map {
        animateFloatAsState(it.toFloat(), tween(450), label = "band").value.roundToInt().toShort()
    }
    ReadOnlyEqualizer(
        modifier = modifier,
        color = neonGreen,
        bands = setting.setting.bandHz,
        values = animated,
        minValue = setting.setting.min,
        maxValue = setting.setting.max,
        fractionDigits = setting.setting.fractionDigits,
    )
}

@Composable
internal fun BassBoostControl(setting: Setting.EqualizerSetting, onChange: (List<Short>) -> Unit) {
    val (shown, setShown) = throttledState(currentBassPercent(setting), 300L) {
        onChange(applyBass(setting, it))
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bass boost", fontWeight = FontWeight.Medium)
            Text(
                text = String.format(Locale.US, "+%.1f dB", shown * BASS_PEAK_TENTHS_DB / 100f / 10f),
                color = neonGreen,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = shown.toFloat(),
            onValueChange = { setShown(it.roundToInt()) },
            valueRange = 0f..100f,
        )
    }
}

@Composable
fun EqualizerStudioScreen(
    settings: List<Pair<String, Setting>>,
    setSettings: (List<Pair<String, Value>>) -> Unit,
    onOpenAllSettings: () -> Unit,
) {
    val byId = settings.toMap()
    val volume = byId[SETTING_VOLUME] as? Setting.EqualizerSetting
    val preset = byId[SETTING_PRESET] as? Setting.PresetEqualizerProfileSelect
    val custom = byId[SETTING_CUSTOM] as? Setting.ModifiableSelectSetting
    val set: (String, Value) -> Unit = { id, value -> setSettings(listOf(id to value)) }

    if (volume == null) {
        Text("Equalizer is not available on this device.", modifier = Modifier.padding(16.dp))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GlassCard(title = "Live response") {
            AnimatedEqualizerCurve(volume, Modifier.fillMaxWidth().height(150.dp))
            Text(
                text = if (preset?.value != null) {
                    "Active preset: " + (
                        preset.select.localizedOptions.getOrNull(preset.select.options.indexOf(preset.value))
                            ?: preset.value
                        )
                } else {
                    "Active: custom curve"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        GlassCard(title = "Bass lab") {
            BassBoostControl(volume) { set(SETTING_VOLUME, it.toValue()) }
            ChipRow(
                title = "Bass profiles",
                options = BASS_PROFILES.map { it.name },
                labels = BASS_PROFILES.map { it.name },
                selectedIndex = BASS_PROFILES.indexOfFirst { fitGains(it.gains, volume) == volume.value },
                onSelect = { set(SETTING_VOLUME, fitGains(BASS_PROFILES[it].gains, volume).toValue()) },
            )
        }

        if (preset != null) {
            GlassCard(title = "Presets") {
                preset.select.options.forEachIndexed { index, option ->
                    PresetRow(
                        label = preset.select.localizedOptions.getOrElse(index) { option },
                        selected = option == preset.value,
                        gains = preset.presets.getOrElse(index) { emptyList() },
                        equalizer = preset.equalizer,
                        onClick = { set(SETTING_PRESET, option.toValue()) },
                    )
                }
            }
        }

        if (custom != null) {
            GlassCard(title = translateSettingId(SETTING_CUSTOM)) {
                ModifiableSelect(
                    name = translateSettingId(SETTING_CUSTOM),
                    showLabel = false,
                    options = custom.setting.options,
                    selectedIndex = custom.setting.options.indexOf(custom.value).let { if (it == -1) null else it },
                    onSelect = { set(SETTING_CUSTOM, custom.setting.options[it].toValue()) },
                    onAddOption = { set(SETTING_CUSTOM, ModifiableSelectCommandInner.Add(it).toValue()) },
                    onRemoveOption = {
                        set(SETTING_CUSTOM, ModifiableSelectCommandInner.Remove(custom.setting.options[it]).toValue())
                    },
                )
            }
        }

        GlassCard(title = "Fine tune bands") {
            Equalizer(
                bands = volume.setting.bandHz,
                values = volume.value,
                minValue = volume.setting.min,
                maxValue = volume.setting.max,
                fractionDigits = volume.setting.fractionDigits,
                onValueChange = { changedIndex, newValue ->
                    set(
                        SETTING_VOLUME,
                        volume.value.mapIndexed { i, old -> if (i == changedIndex) newValue else old }.toValue(),
                    )
                },
            )
        }

        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenAllSettings) {
            Text("Import / export profiles & all settings")
        }
    }
}

@Composable
private fun PresetRow(
    label: String,
    selected: Boolean,
    gains: List<Short>,
    equalizer: com.oppzippy.openscq30.lib.wrapper.Equalizer,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val bg by animateColorAsState(if (selected) Color(0xFF0B3D22) else Color(0xFF0E1511), tween(300), label = "presetBg")
    val borderColor by animateColorAsState(
        if (selected) neonGreen else Color(0xFF1E2E25),
        tween(300),
        label = "presetBorder",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            if (selected) Text("Active", color = neonGreen, style = MaterialTheme.typography.labelSmall)
        }
        if (gains.isNotEmpty()) {
            ReadOnlyEqualizer(
                modifier = Modifier.width(120.dp).height(44.dp),
                color = if (selected) neonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                bands = equalizer.bandHz,
                values = gains,
                minValue = equalizer.min,
                maxValue = equalizer.max,
                fractionDigits = equalizer.fractionDigits,
                drawHorizontalGuide = false,
            )
        }
    }
}
