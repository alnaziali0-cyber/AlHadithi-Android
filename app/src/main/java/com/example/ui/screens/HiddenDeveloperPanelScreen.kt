package com.example.ui.screens

import android.widget.Toast
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LockPerson
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.local.AdminAnnouncementEntity
import com.example.data.local.AudioTrackEntity
import com.example.data.local.BookEntity
import com.example.data.local.DhikrEntity
import com.example.data.local.DuaZiyaratEntity
import com.example.data.local.HadithEntity
import com.example.data.local.IslamicOccasionEntity
import com.example.data.local.WallpaperEntity
import com.example.data.remote.AUTHORIZED_DEVELOPER_EMAIL
import com.example.data.remote.DeveloperGoogleAuthHelper
import com.example.ui.components.EmeraldHeroCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassFilterChip
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.HeritageGold
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Hidden Developer & Owner Control Panel revealed ONLY by shaking the phone (or 7-tap logo gesture).
 * Strictly locked to the configured verified owner identity through Firebase Authentication.
 * Google and email/password providers are accepted only when the verified owner check succeeds.
 * Uploaded wallpapers are stored in this installation's private app storage in this release.
 */
@Composable
fun HiddenDeveloperPanelScreen(
    authorizedUser: FirebaseUser?,
    announcements: List<AdminAnnouncementEntity>,
    hadiths: List<HadithEntity>,
    duasAndZiyarat: List<DuaZiyaratEntity>,
    adhkar: List<DhikrEntity>,
    books: List<BookEntity>,
    audioTracks: List<AudioTrackEntity>,
    occasions: List<IslamicOccasionEntity>,
    wallpapers: List<WallpaperEntity>,
    onBack: () -> Unit,
    onAddAnnouncement: (String, String, String, String, (Boolean, String) -> Unit) -> Unit,
    onToggleAnnouncementActive: (AdminAnnouncementEntity, Boolean) -> Unit,
    onDeleteAnnouncement: (AdminAnnouncementEntity) -> Unit,
    onAddHadith: (HadithEntity) -> Unit,
    onDeleteHadith: (Long) -> Unit,
    onAddDuaZiyarat: (DuaZiyaratEntity) -> Unit,
    onDeleteDuaZiyarat: (Long) -> Unit,
    onAddDhikr: (DhikrEntity) -> Unit,
    onDeleteDhikr: (Long) -> Unit,
    onAddBook: (BookEntity) -> Unit,
    onDeleteBook: (Long) -> Unit,
    onAddAudio: (AudioTrackEntity) -> Unit,
    onDeleteAudio: (Long) -> Unit,
    onAddOccasion: (IslamicOccasionEntity) -> Unit,
    onDeleteOccasion: (Long) -> Unit,
    onAddWallpaper: (WallpaperEntity) -> Unit,
    onDeleteWallpaper: (Long) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var authErrorBanner by remember { mutableStateOf<String?>(null) }
    var rejectedAccessBanner by remember { mutableStateOf<String?>(null) }
    var developerEmailInput by remember { mutableStateOf(AUTHORIZED_DEVELOPER_EMAIL) }
    var developerPasswordInput by remember { mutableStateOf("") }

    // Email/password is the primary path. No automatic account chooser is opened on entry;
    // the optional Google flow remains available as an explicit secondary action.

    // --- GATE: Require a verified Firebase session for the configured owner only (Google or Email/Password).
    if (authorizedUser == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = "لوحة المطور المخفية · DEVELOPER",
                subtitle = "بوابة إدارة خاصة — تحقق آمن من حساب المطوّر",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("developer_auth_gate"),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    EmeraldHeroCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.6f)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Vibration,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "بوابة المطور والإدارة السحابية",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "هذه اللوحة مخفية تماماً عن المستخدمين وتعمل حصرياً لحساب المطور المربوط.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }

                item {
                    GlassCard(cornerRadius = 24.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LockPerson,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "تسجيل دخول المطوّر",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "يُسمح بالدخول للحساب المصرّح به فقط، مع التحقق من البريد عبر Firebase:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "التحقق يتم عبر Firebase · لا تُحفظ كلمة المرور داخل التطبيق",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (rejectedAccessBanner != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = rejectedAccessBanner!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (authErrorBanner != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = authErrorBanner!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = developerEmailInput,
                            onValueChange = { developerEmailInput = it.take(160) },
                            label = { Text("البريد الإلكتروني") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("developer_email_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = developerPasswordInput,
                            onValueChange = { developerPasswordInput = it.take(256) },
                            label = { Text("كلمة المرور") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("developer_password_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            enabled = !isSigningIn && developerEmailInput.isNotBlank() && developerPasswordInput.isNotEmpty(),
                            onClick = {
                                if (isSigningIn) return@Button
                                isSigningIn = true
                                authErrorBanner = null
                                rejectedAccessBanner = null
                                val enteredEmail = developerEmailInput
                                val enteredPassword = developerPasswordInput
                                DeveloperGoogleAuthHelper.signInWithEmailPassword(
                                    email = enteredEmail,
                                    password = enteredPassword,
                                    scope = scope,
                                    onAuthorizedSuccess = {
                                        isSigningIn = false
                                        developerPasswordInput = ""
                                        Toast.makeText(context, "تم التحقق من حساب المطوّر", Toast.LENGTH_SHORT).show()
                                    },
                                    onUnauthorizedAccount = {
                                        isSigningIn = false
                                        developerPasswordInput = ""
                                        rejectedAccessBanner = "الحساب غير مخوّل أو لم يتم توثيق البريد الإلكتروني."
                                    },
                                    onAuthError = { message ->
                                        isSigningIn = false
                                        developerPasswordInput = ""
                                        authErrorBanner = message
                                    }
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("developer_email_password_sign_in")
                        ) {
                            if (isSigningIn) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("دخول آمن للوحة المطوّر", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "يتطلب تفعيل مزوّد Email/Password في Firebase وإنشاء حساب المطوّر وتوثيق بريده. لا تضع كلمة المرور داخل الكود أو ملفات الإعدادات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (isSigningIn) return@Button
                                isSigningIn = true
                                authErrorBanner = null
                                rejectedAccessBanner = null
                                DeveloperGoogleAuthHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onAuthorizedSuccess = {
                                        isSigningIn = false
                                        Toast.makeText(context, "مرحباً بك في لوحة المطور ✓", Toast.LENGTH_SHORT).show()
                                    },
                                    onUnauthorizedAccount = {
                                        isSigningIn = false
                                        rejectedAccessBanner = "تم رفض تسجيل الدخول؛ الحساب المختار غير مخوّل باستخدام لوحة المطوّر."
                                    },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        authErrorBanner = msg
                                    },
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    }
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("developer_google_sign_in_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("جارٍ التحقق من حساب المطور…")
                            } else {
                                Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الدخول عبر Google للمطور",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // --- UNLOCKED DEVELOPER & ANNOUNCEMENTS PANEL (verified owner session) ---
    var sectionTab by remember { mutableIntStateOf(0) }
    // 0 = الإعلانات والتنبيهات, 1 = الصوتيات الحسينية, 2 = الأحاديث, 3 = الأدعية والزيارات, 4 = الأذكار, 5 = الكتب, 6 = المناسبات

    // Announcement Form States
    var adTitle by remember { mutableStateOf("") }
    var adBody by remember { mutableStateOf("") }
    var adBadge by remember { mutableStateOf("إعلان هام") }
    var adUrl by remember { mutableStateOf("") }

    // Encyclopedia CMS Form States
    var fTitle by remember { mutableStateOf("") }
    var fSubtitleOrSource by remember { mutableStateOf("") }
    var fCategory by remember { mutableStateOf("") }
    var fBodyOrUrl by remember { mutableStateOf("") }
    var wallpaperImportBusy by remember { mutableStateOf(false) }

    fun clearCmsForm() {
        fTitle = ""
        fSubtitleOrSource = ""
        fCategory = ""
        fBodyOrUrl = ""
    }

    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && !wallpaperImportBusy) {
            wallpaperImportBusy = true
            scope.launch {
                val imported = importWallpaperImage(context, uri)
                wallpaperImportBusy = false
                if (imported == null) {
                    Toast.makeText(context, "تعذر استيراد الصورة. استخدم JPG أو PNG بحجم أقل من 15 MB.", Toast.LENGTH_LONG).show()
                } else {
                    onAddWallpaper(
                        WallpaperEntity(
                            title = fTitle.trim().ifBlank { "خلفية إسلامية جديدة" }.take(100),
                            category = fCategory.trim().ifBlank { "خلفيات إسلامية" }.take(60),
                            drawableResName = "",
                            resolutionLabel = "${File(imported).length() / 1024} KB",
                            description = fSubtitleOrSource.trim().ifBlank { "أضيفت من لوحة المطور" }.take(300),
                            localImagePath = imported
                        )
                    )
                    clearCmsForm()
                    Toast.makeText(context, "تمت إضافة الخلفية إلى التطبيق", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = "لوحة المطور · AL-HADITHI",
            subtitle = "جلسة المطوّر محمية · تحقق موثّق عبر Firebase",
            onBack = onBack,
            trailing = {
                OutlinedButton(
                    onClick = {
                        DeveloperGoogleAuthHelper.signOutDeveloper(credentialManager, scope) {
                            Toast.makeText(context, "تم قفل لوحة المطور وتسجيل الخروج", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("developer_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("خروج")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("developer_unlocked_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Verified Developer Identity Card
            item {
                GlassCard(cornerRadius = 20.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "حساب المطور الموثق نشط ✓",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "الإعلانات (${announcements.size}) · الصوتيات (${audioTracks.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Section Tabs
            item {
                val tabs = listOf(
                    "الإعلانات والتنبيهات",
                    "الصوتيات الحسينية",
                    "الأحاديث",
                    "الأدعية والزيارات",
                    "الأذكار",
                    "الكتب والسيرة",
                    "المناسبات",
                    "الخلفيات"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs.size) { idx ->
                        GlassFilterChip(
                            label = tabs[idx],
                            selected = sectionTab == idx,
                            onClick = {
                                sectionTab = idx
                                clearCmsForm()
                            }
                        )
                    }
                }
            }

            if (sectionTab == 0) {
                // --- TAB 0: ADD & DELETE ANNOUNCEMENTS / ADS ---
                item {
                    GlassCard(cornerRadius = 22.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "إضافة إعلان أو تنبيه زجاجي في الرئيسية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = adTitle,
                            onValueChange = { adTitle = it },
                            label = { Text("عنوان الإعلان (مثال: مجلس عزاء مركزي / تحديث جديد)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dev_ad_title_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adBadge,
                            onValueChange = { adBadge = it },
                            label = { Text("شارة الإعلان (مثال: إعلان هام · بث مباشر)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adBody,
                            onValueChange = { adBody = it },
                            label = { Text("نص الإعلان التفصيلي") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dev_ad_body_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = adUrl,
                            onValueChange = { adUrl = it },
                            label = { Text("رابط خارجي اختياري عند الضغط (https://...)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (adTitle.isBlank() || adBody.isBlank()) {
                                    Toast.makeText(context, "يرجى كتابة عنوان ونص الإعلان", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onAddAnnouncement(adTitle, adBody, adBadge, adUrl) { _, msg ->
                                    adTitle = ""
                                    adBody = ""
                                    adUrl = ""
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dev_publish_ad_button")
                        ) {
                            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("نشر الإعلان الآن", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (announcements.isEmpty()) {
                    item {
                        GlassCard {
                            Text(
                                text = "لا توجد إعلانات منشورة حالياً. أضف إعلاناً أعلاه ليظهر ببطاقة زجاجية شفافة في الصفحة الرئيسية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(announcements, key = { it.id }) { ad ->
                        GlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                ) {
                                    Text(
                                        text = ad.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (ad.isActive) "مفعّل" else "مخفي",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = ad.isActive,
                                        onCheckedChange = { onToggleAnnouncementActive(ad, it) }
                                    )
                                    IconButton(
                                        onClick = {
                                            onDeleteAnnouncement(ad)
                                            Toast.makeText(context, "تم حذف الإعلان نهائياً", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("delete_ad_${ad.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "حذف الإعلان",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ad.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ad.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (sectionTab == 7) {
                item {
                    GlassCard(cornerRadius = 22.dp) {
                        Text("إدارة خلفيات التطبيق", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("يتم نسخ الصور إلى مساحة التطبيق الخاصة والتحقق من نوعها وحجمها، ولا تُرفع إلى الإنترنت تلقائياً.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = fTitle, onValueChange = { fTitle = it.take(100) }, label = { Text("عنوان الخلفية") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(value = fCategory, onValueChange = { fCategory = it.take(60) }, label = { Text("التصنيف") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(value = fSubtitleOrSource, onValueChange = { fSubtitleOrSource = it.take(300) }, label = { Text("وصف مختصر") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Button(
                            enabled = !wallpaperImportBusy,
                            onClick = { wallpaperPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (wallpaperImportBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("اختيار صورة وإضافتها إلى الخلفيات")
                        }
                    }
                }
                items(wallpapers, key = { "wallpaper_${it.id}" }) { wallpaper ->
                    DeveloperCmsRow(wallpaper.title, "${wallpaper.category} · ${wallpaper.resolutionLabel}") { onDeleteWallpaper(wallpaper.id) }
                }
            } else {
                // --- TABS 1..6: ENCYCLOPEDIA & HUSSEINI AUDIO MANAGEMENT ---
                item {
                    GlassCard {
                        Text(
                            text = "إضافة عنصر جديد للموسوعة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fTitle,
                            onValueChange = { fTitle = it },
                            label = {
                                Text(
                                    when (sectionTab) {
                                        1 -> "عنوان القصيدة أو المقطع الصوتي"
                                        2 -> "المعصوم القائل (مثال: الإمام الصادق ع)"
                                        else -> "العنوان"
                                    }
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = fCategory,
                            onValueChange = { fCategory = it },
                            label = {
                                Text(
                                    when (sectionTab) {
                                        1 -> "التصنيف (اللطميات والقصائد / النعي الحسيني / الأدعية والمناجاة)"
                                        3 -> "النوع (DUA للدعاء أو ZIYARAT للزيارة)"
                                        6 -> "الشهر واليوم الهجري (مثال: 7/13)"
                                        else -> "التصنيف"
                                    }
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = fSubtitleOrSource,
                            onValueChange = { fSubtitleOrSource = it },
                            label = {
                                Text(
                                    when (sectionTab) {
                                        1 -> "اسم الرادود أو القارئ"
                                        else -> "المصدر المعتمد والمؤلف"
                                    }
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = fBodyOrUrl,
                            onValueChange = { fBodyOrUrl = it },
                            label = {
                                Text(
                                    when (sectionTab) {
                                        1 -> "رابط MP3 المباشر (https://...)"
                                        else -> "النص الكامل المضبوط"
                                    }
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (fTitle.isBlank() || fBodyOrUrl.isBlank()) {
                                    Toast.makeText(context, "يرجى ملء الحقول الأساسية", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                when (sectionTab) {
                                    1 -> onAddAudio(
                                        AudioTrackEntity(
                                            category = fCategory.ifBlank { "اللطميات والقصائد" },
                                            title = fTitle.trim(),
                                            reciterOrRadood = fSubtitleOrSource.ifBlank { "رادود حسيني" },
                                            reciterBio = "مضاف من لوحة المطور",
                                            streamUrl = fBodyOrUrl.trim(),
                                            durationLabel = "صوتيات"
                                        )
                                    )
                                    2 -> onAddHadith(
                                        HadithEntity(
                                            arabicText = fBodyOrUrl.trim(),
                                            infallibleName = fTitle.trim(),
                                            topicCategory = fCategory.ifBlank { "الحكمة والأخلاق" },
                                            sourceBook = fSubtitleOrSource.ifBlank { "مصدر موثق" },
                                            authorName = "مضاف من لوحة المطور",
                                            volumeAndPage = "نسخة محققة",
                                            gradingNote = "موثق",
                                            briefExplanation = ""
                                        )
                                    )
                                    3 -> onAddDuaZiyarat(
                                        DuaZiyaratEntity(
                                            entryType = if (fCategory.trim().uppercase() == "ZIYARAT") "ZIYARAT" else "DUA",
                                            category = "مضاف من الإدارة",
                                            title = fTitle.trim(),
                                            vocalizedText = fBodyOrUrl.trim(),
                                            attribution = "مأثور عن أهل البيت (ع)",
                                            sourceBook = fSubtitleOrSource.ifBlank { "مفاتيح الجنان" },
                                            recommendedTime = "في جميع الأوقات"
                                        )
                                    )
                                    4 -> onAddDhikr(
                                        DhikrEntity(
                                            category = fCategory.ifBlank { "أذكار عامة" },
                                            title = fTitle.trim(),
                                            vocalizedText = fBodyOrUrl.trim(),
                                            targetCount = 33,
                                            virtueAndSource = fSubtitleOrSource.ifBlank { "مفاتيح الجنان" }
                                        )
                                    )
                                    5 -> onAddBook(
                                        BookEntity(
                                            sectionType = "LIBRARY",
                                            category = fCategory.ifBlank { "العقائد" },
                                            title = fTitle.trim(),
                                            author = fSubtitleOrSource.ifBlank { "مؤلف معتمد" },
                                            eraLabel = "نسخة محققة",
                                            summary = fBodyOrUrl.take(140),
                                            sourceAttribution = fSubtitleOrSource.ifBlank { "طبعة معتمدة" },
                                            chaptersDelimited = "${fTitle.trim()}::${fBodyOrUrl.trim()}"
                                        )
                                    )
                                    6 -> {
                                        val parts = fCategory.split("/")
                                        val hm = parts.getOrNull(0)?.trim()?.toIntOrNull()?.coerceIn(1, 12) ?: 1
                                        val hd = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceIn(1, 30) ?: 1
                                        onAddOccasion(
                                            IslamicOccasionEntity(
                                                hijriMonth = hm,
                                                hijriDay = hd,
                                                eventType = "COMMEMORATION",
                                                title = fTitle.trim(),
                                                infallibleOrTopic = fSubtitleOrSource.ifBlank { "أهل البيت (ع)" },
                                                description = fBodyOrUrl.trim()
                                            )
                                        )
                                    }
                                }
                                clearCmsForm()
                                Toast.makeText(context, "تم الحفظ بنجاح ✓", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ ونشر")
                        }
                    }
                }

                when (sectionTab) {
                    1 -> items(audioTracks, key = { it.id }) { t ->
                        DeveloperCmsRow(t.title, "${t.reciterOrRadood} · ${t.category}") { onDeleteAudio(t.id) }
                    }
                    2 -> items(hadiths, key = { it.id }) { h ->
                        DeveloperCmsRow(h.infallibleName, h.arabicText) { onDeleteHadith(h.id) }
                    }
                    3 -> items(duasAndZiyarat, key = { it.id }) { d ->
                        DeveloperCmsRow(d.title, d.sourceBook) { onDeleteDuaZiyarat(d.id) }
                    }
                    4 -> items(adhkar, key = { it.id }) { a ->
                        DeveloperCmsRow(a.title, a.category) { onDeleteDhikr(a.id) }
                    }
                    5 -> items(books, key = { it.id }) { b ->
                        DeveloperCmsRow(b.title, b.author) { onDeleteBook(b.id) }
                    }
                    6 -> items(occasions, key = { it.id }) { o ->
                        DeveloperCmsRow(o.title, "${o.hijriDay}/${o.hijriMonth} هـ") { onDeleteOccasion(o.id) }
                    }
                }
            }
        }
    }
}

private suspend fun importWallpaperImage(context: android.content.Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "wallpapers")
        val target = File(directory, "wallpaper_${UUID.randomUUID()}.img")
        var keepFile = false
        try {
            if (!directory.exists() && !directory.mkdirs()) return@withContext null
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            if (mimeType !in setOf("image/jpeg", "image/png", "image/webp")) return@withContext null
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output, 8192) }
            } ?: return@withContext null
            if (target.length() !in 1_024L..(15L * 1_024L * 1_024L)) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            if (bounds.outWidth !in 1..8_000 || bounds.outHeight !in 1..8_000 ||
                bounds.outMimeType !in setOf("image/jpeg", "image/png", "image/webp")) return@withContext null
            keepFile = true
            target.absolutePath
        } catch (_: Exception) {
            null
        } finally {
            if (!keepFile) target.delete()
        }
    }

@Composable
private fun DeveloperCmsRow(title: String, subtitle: String, onDelete: () -> Unit) {
    GlassCard(cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "حذف العنصر",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
