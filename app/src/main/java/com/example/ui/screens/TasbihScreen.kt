package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.TasbihSessionEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.TurquoisePrimaryLight
import com.example.ui.theme.TurquoiseSecondaryLight
import com.example.util.SystemActionsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TasbihPreset(
    val id: String,
    val title: String,
    val dhikrFormula: String,
    val target: Int, // 0 means open/unbounded
    val isZahraSequence: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasbihScreen(
    hapticEnabled: Boolean,
    dailyGoal: Int,
    dailyCount: Int,
    sessions: List<TasbihSessionEntity>,
    onBack: () -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onChangeDailyGoal: (Int) -> Unit,
    onTickDailyCount: () -> Unit,
    onSaveSession: (String, String, Int, Int) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val presets = remember {
        listOf(
            TasbihPreset("zahra", "تسبيح الزهراء (ع)", "اللهُ أَكْبَرُ · الْحَمْدُ لِلَّهِ · سُبْحَانَ اللهِ", 100, isZahraSequence = true),
            TasbihPreset("subhan33", "سبحان الله ×٣٣", "سُبْحَانَ اللهِ", 33),
            TasbihPreset("hamd33", "الحمد لله ×٣٣", "الْحَمْدُ لِلَّهِ", 33),
            TasbihPreset("akbar34", "الله أكبر ×٣٤", "اللهُ أَكْبَرُ", 34),
            TasbihPreset("istighfar100", "أستغفر الله ×١٠٠", "أَسْتَغْفِرُ اللهَ رَبِّي وَأَتُوبُ إِلَيْهِ", 100),
            TasbihPreset("tahlil100", "لا إله إلا الله ×١٠٠", "لَا إِلَهَ إِلَّا اللهُ", 100),
            TasbihPreset("salawat100", "الصلاة على محمد وآل محمد ×١٠٠", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَآلِ مُحَمَّدٍ", 100),
            TasbihPreset("open", "عدّ مفتوح", "ذِكْرُ اللهِ تَعَالَى", 0),
            TasbihPreset("custom", "عدّ مخصص", "وِرْدٌ مَخْصُوصٌ", 70)
        )
    }

    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var customTarget by remember { mutableIntStateOf(70) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("70") }

    // State for standard or Fatima al-Zahra 3-stage sequence (0: 34 Akbar, 1: 33 Hamd, 2: 33 Subhan)
    var stageIndex by remember { mutableIntStateOf(0) }
    var stageCount by remember { mutableIntStateOf(0) }
    var totalSessionCount by remember { mutableIntStateOf(0) }
    var completedBannerVisible by remember { mutableStateOf(false) }

    val zahraStages = listOf(
        "اللهُ أَكْبَرُ" to 34,
        "الْحَمْدُ لِلَّهِ" to 33,
        "سُبْحَانَ اللهِ" to 33
    )

    val activeDhikrLabel = if (selectedPreset.isZahraSequence) {
        zahraStages[stageIndex].first
    } else {
        selectedPreset.dhikrFormula
    }

    val activeStageTarget = when {
        selectedPreset.isZahraSequence -> zahraStages[stageIndex].second
        selectedPreset.id == "custom" -> customTarget
        else -> selectedPreset.target
    }

    val progressFraction = when {
        selectedPreset.isZahraSequence -> (totalSessionCount / 100f).coerceIn(0f, 1f)
        activeStageTarget > 0 -> (stageCount.toFloat() / activeStageTarget.toFloat()).coerceIn(0f, 1f)
        else -> ((totalSessionCount % 100) / 100f)
    }

    val animatedArc by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tasbih_arc"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val circleScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tasbih_press"
    )

    fun resetCurrentSession() {
        stageIndex = 0
        stageCount = 0
        totalSessionCount = 0
        completedBannerVisible = false
    }

    fun handleTap() {
        if (completedBannerVisible && selectedPreset.isZahraSequence) {
            resetCurrentSession()
        }
        onTickDailyCount()

        if (selectedPreset.isZahraSequence) {
            stageCount++
            totalSessionCount++
            val targetForStage = zahraStages[stageIndex].second
            if (stageCount >= targetForStage) {
                if (stageIndex < 2) {
                    // Auto-advance to next stage of Tasbih al-Zahra!
                    stageIndex++
                    stageCount = 0
                    if (hapticEnabled) SystemActionsHelper.triggerHapticPulse(context, strongMilestone = true)
                } else {
                    // Completed full 100 (34 + 33 + 33)!
                    completedBannerVisible = true
                    if (hapticEnabled) SystemActionsHelper.triggerHapticPulse(context, strongMilestone = true)
                    onSaveSession("تسبيح الزهراء (ع)", "٣٤ تكبيرة · ٣٣ تحميدة · ٣٣ تسبيحة", 100, 100)
                }
            } else {
                if (hapticEnabled) SystemActionsHelper.triggerHapticPulse(context, strongMilestone = false)
            }
        } else {
            stageCount++
            totalSessionCount++
            if (activeStageTarget > 0 && stageCount == activeStageTarget) {
                completedBannerVisible = true
                if (hapticEnabled) SystemActionsHelper.triggerHapticPulse(context, strongMilestone = true)
                onSaveSession(selectedPreset.title, activeDhikrLabel, stageCount, activeStageTarget)
            } else {
                if (hapticEnabled) SystemActionsHelper.triggerHapticPulse(context, strongMilestone = false)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "المسبحة الإلكترونية المطورة",
            subtitle = "تسبيح السيدة فاطمة الزهراء (ع) والأوراد اليومية",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stage indicator pills when in Tasbih al-Zahra mode
            if (selectedPreset.isZahraSequence) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        zahraStages.forEachIndexed { idx, (label, count) ->
                            val isCurrent = idx == stageIndex && !completedBannerVisible
                            val isPassed = idx < stageIndex || completedBannerVisible
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isPassed -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = "$label ($count)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                                        isPassed -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Completion Celebration Banner
            if (completedBannerVisible) {
                item {
                    GlassCard(
                        cornerRadius = 20.dp,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تقبّل الله طاعتكم — اكتمل التسبيح المبارك!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "حُفظت الجلسة تلقائياً في سجل تسبيحاتك. اضغط الدائرة للبدء من جديد.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Central Glassmorphic Circular Tasbih Button with Animated Progress Ring
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(276.dp)
                        .graphicsLayer {
                            scaleX = circleScale
                            scaleY = circleScale
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple(),
                            onClick = { handleTap() }
                        )
                        .testTag("tasbih_central_circle_button")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 12.dp.toPx()
                        val radius = (size.minDimension - strokePx * 2) / 2f
                        // Inner spiritual emerald gradient disc
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    TurquoiseSecondaryLight,
                                    TurquoisePrimaryLight,
                                    EmeraldDeep
                                )
                            ),
                            radius = radius - 8.dp.toPx()
                        )
                        // Subtle track ring
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.18f),
                            radius = radius,
                            style = Stroke(width = strokePx)
                        )
                        // Gold/Turquoise active progress arc
                        drawArc(
                            color = HeritageGold,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedArc,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = activeDhikrLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedPreset.isZahraSequence && completedBannerVisible) "100"
                            else stageCount.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDF2D0)
                        )
                        Text(
                            text = when {
                                selectedPreset.isZahraSequence -> "المرحلة: $stageCount / $activeStageTarget · المجموع: $totalSessionCount / 100"
                                activeStageTarget > 0 -> "من $activeStageTarget"
                                else -> "عدّ مفتوح · المس للمتابعة"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Reset & Manual Save Row
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { resetCurrentSession() },
                        modifier = Modifier.testTag("tasbih_reset_button")
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("تصفير")
                    }
                    Button(
                        onClick = {
                            onSaveSession(
                                selectedPreset.title,
                                activeDhikrLabel,
                                totalSessionCount,
                                if (selectedPreset.isZahraSequence) 100 else activeStageTarget
                            )
                            resetCurrentSession()
                        },
                        enabled = totalSessionCount > 0,
                        modifier = Modifier.testTag("tasbih_save_button")
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("حفظ الجلسة")
                    }
                }
            }

            // Daily Goal Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الهدف اليومي للتسبيح",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$dailyCount من $dailyGoal تسبيحة اليوم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onChangeDailyGoal((dailyGoal - 50).coerceAtLeast(33)) }) {
                                Text("−50")
                            }
                            Text("$dailyGoal", fontWeight = FontWeight.Bold)
                            TextButton(onClick = { onChangeDailyGoal(dailyGoal + 50) }) {
                                Text("+50")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (dailyCount.toFloat() / dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Presets Selector
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "أوضاع التسبيح والأذكار",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            GlassFilterChip(
                                label = preset.title,
                                selected = selectedPreset.id == preset.id,
                                onClick = {
                                    selectedPreset = preset
                                    resetCurrentSession()
                                    if (preset.id == "custom") {
                                        showCustomDialog = true
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الاهتزاز اللمسي عند العدّ والإكمال", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = hapticEnabled, onCheckedChange = onToggleHaptic)
                    }
                }
            }

            // Saved Sessions Log
            item {
                Text(
                    text = "سجل الجلسات المحفوظة (${sessions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (sessions.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "لا توجد جلسات محفوظة بعد — تُحفظ تلقائياً عند إكمال التسبيح أو بزر «حفظ الجلسة».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(sessions.take(15), key = { it.id }) { session ->
                    val dateStr = remember(session.completedAtMillis) {
                        SimpleDateFormat("yyyy/MM/dd · hh:mm a", Locale("ar")).format(Date(session.completedAtMillis))
                    }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.modeTitle, fontWeight = FontWeight.Bold)
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${session.completedCount} تسبيحة",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("تحديد العدد المخصص") },
            text = {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("عدد التسبيحات المستهدف") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    customTarget = (customInput.toIntOrNull() ?: 70).coerceIn(1, 10000)
                    showCustomDialog = false
                }) { Text("اعتماد") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
