package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.IraqDistricts
import com.example.domain.IraqGovernorates
import com.example.util.SystemActionsHelper
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GovernorateSelectorSheet(
    currentGovernorateId: String,
    currentDistrictId: String?,
    hapticEnabled: Boolean,
    notifyPrayersEnabled: Boolean,
    onSelectGovernorate: (String) -> Unit,
    onSelectDistrict: (String?) -> Unit,
    onDetectedCoordinates: (Double, Double) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleNotifyPrayers: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pendingGovId by remember(currentGovernorateId) { mutableStateOf(currentGovernorateId) }
    var pendingDistrictId by remember(currentDistrictId, currentGovernorateId) { mutableStateOf(currentDistrictId) }
    var statusNote by remember { mutableStateOf<String?>(null) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleNotifyPrayers(granted)
    }

    @SuppressLint("MissingPermission")
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val nearest = IraqGovernorates.nearestTo(loc.latitude, loc.longitude)
                        pendingGovId = nearest.id
                        onDetectedCoordinates(loc.latitude, loc.longitude)
                        statusNote = "تم تحديد أقرب محافظة لموقعك: ${nearest.arabicName}"
                    } else {
                        statusNote = "تعذر قراءة إحداثيات GPS حالياً — يمكنك اختيار المحافظة يدوياً"
                    }
                }
        } else {
            statusNote = "لم يُمنح إذن الموقع — يعمل التطبيق كاملاً باختيار المحافظة يدوياً"
        }
    }

    val isDark = com.example.ui.theme.LocalIsDarkThemeActive.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF102422).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_alhadithi_logo),
                        contentDescription = stringResource(R.string.cd_logo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(1.dp)
                    )
                }
                Column {
                    Text(
                        text = "الحديثي | AL-HADITHI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.select_governorate_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "محافظتك العراقية المعتمدة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = {
                        locationPermLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("use_gps_governorate_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.use_my_location))
                }
            }

            if (statusNote != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusNote!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IraqGovernorates.all.forEach { gov ->
                    GlassFilterChip(
                        label = gov.arabicName,
                        selected = pendingGovId == gov.id,
                        onClick = { pendingGovId = gov.id },
                        modifier = Modifier.testTag("gov_option_${gov.id}")
                    )
                }
            }

            // اختيار المنطقة/المدينة داخل المحافظة — يحسّن دقة المواقيت.
            val districts = IraqDistricts.of(pendingGovId)
            if (districts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "اختر منطقتك أو مدينتك (اختياري)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    districts.forEach { district ->
                        GlassFilterChip(
                            label = district.arabicName,
                            selected = pendingDistrictId == district.id,
                            onClick = {
                                pendingDistrictId = district.id
                                onSelectDistrict(district.id)
                            },
                            modifier = Modifier.testTag("district_option_${district.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(cornerRadius = 18.dp) {
                Text(
                    text = "الأذونات الاختيارية (يمكن رفض أي إذن دون تأثر التطبيق)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("تنبيهات مواقيت الصلاة", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = notifyPrayersEnabled,
                        onCheckedChange = { wantEnable ->
                            if (wantEnable && android.os.Build.VERSION.SDK_INT >= 33 &&
                                !SystemActionsHelper.hasNotificationPermission(context)
                            ) {
                                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onToggleNotifyPrayers(wantEnable)
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اهتزاز لطيف عند التسبيح", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = onToggleHaptic
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            val chosenName = IraqGovernorates.byId(pendingGovId).arabicName
            Button(
                onClick = {
                    onSelectGovernorate(pendingGovId)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_governorate_button")
            ) {
                Text(
                    text = "ابدأ — $chosenName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}
