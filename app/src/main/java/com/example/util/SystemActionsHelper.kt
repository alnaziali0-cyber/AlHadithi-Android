package com.example.util

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.R

object SystemActionsHelper {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "تم النسخ إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    fun sharePlainText(context: Context, title: String, body: String) {
        val shareText = "$body\n\n— عبر تطبيق خزائن الهدى | KHAZAIN AL-HUDA"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة عبر"))
    }

    fun triggerHapticPulse(context: Context, strongMilestone: Boolean = false) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                val durationMs = if (strongMilestone) 95L else 24L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amp = if (strongMilestone) 220 else 90
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amp))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun saveDrawableWallpaperToGallery(
        context: Context,
        drawableResId: Int,
        displayName: String
    ): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId) ?: return false
            val filename = "KhazainAlHuda_${displayName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KhazainAlHuda")
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "تم حفظ الخلفية في معرض الصور بنجاح", Toast.LENGTH_SHORT).show()
            true
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر حفظ الصورة على هذا الجهاز", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun saveImageFileToGallery(context: Context, filePath: String, displayName: String): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            var sample = 1
            while (bounds.outWidth / sample > 3072 || bounds.outHeight / sample > 3072) sample *= 2
            val bitmap = BitmapFactory.decodeFile(
                filePath,
                BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
            ) ?: return false
            val safeName = displayName.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(60)
            val filename = "KhazainAlHuda_${safeName}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KhazainAlHuda")
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: run {
                bitmap.recycle()
                return false
            }
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) error("Image encoding failed")
                } ?: error("Image output unavailable")
            } finally {
                bitmap.recycle()
            }
            Toast.makeText(context, "تم حفظ الخلفية في معرض الصور بنجاح", Toast.LENGTH_SHORT).show()
            true
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر حفظ الصورة على هذا الجهاز", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }



}
