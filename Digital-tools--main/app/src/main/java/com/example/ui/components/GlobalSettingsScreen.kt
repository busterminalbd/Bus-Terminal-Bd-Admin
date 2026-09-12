package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import com.example.util.AppBackupManager
import com.example.util.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.ui.theme.HeadingFontFamily
import com.example.ui.theme.ThemePalettes
import com.example.update.AppUpdateManager
import com.example.update.UpdateDialog
import com.example.update.UpdateInfo
import com.example.util.BengaliUtils
import kotlinx.coroutines.launch

@Composable
fun GlobalSettingsScreen(
    themeMode: String = "system",
    themeColor: String = "emerald",
    appLanguage: String = "bn",
    fontScale: Float = 1.0f,
    onThemeModeChange: (String) -> Unit = {},
    onThemeColorChange: (String) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onFontScaleChange: (Float) -> Unit = {},
    onOpenMemoSettings: () -> Unit = {},
    onDataImported: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Backup & Restore states
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var cachedJsonToExport by remember { mutableStateOf<String?>(null) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }
    var importResultToShow by remember { mutableStateOf<ImportResult?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && cachedJsonToExport != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(cachedJsonToExport!!.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        exportSuccessMessage = if (appLanguage == "en") {
                            "Backup file downloaded and saved successfully!"
                        } else {
                            "ব্যাকআপ ফাইলটি সফলভাবে ডাউনলোড ও সেভ করা হয়েছে!"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        exportErrorMessage = if (appLanguage == "en") "Download failed: ${e.localizedMessage}" else "ডাউনলোড ব্যর্থ হয়েছে: ${e.localizedMessage}"
                    }
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isImporting = true
                try {
                    val jsonString = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    if (jsonString.isNullOrBlank()) {
                        importResultToShow = ImportResult(
                            success = false,
                            message = if (appLanguage == "en") "The selected backup file is empty." else "নির্বাচিত ব্যাকআপ ফাইলটিতে কোনো ডাটা পাওয়া যায়নি।"
                        )
                    } else {
                        val result = AppBackupManager.importAllData(context, jsonString)
                        importResultToShow = result
                        if (result.success) {
                            onDataImported()
                        }
                    }
                } catch (e: Exception) {
                    importResultToShow = ImportResult(
                        success = false,
                        message = if (appLanguage == "en") "Failed to read backup file: ${e.localizedMessage}" else "ব্যাকআপ ফাইল পড়তে সমস্যা হয়েছে: ${e.localizedMessage}"
                    )
                } finally {
                    isImporting = false
                }
            }
        }
    }

    // App Update States
    val updateManager = remember { AppUpdateManager() }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var noUpdateMessage by remember { mutableStateOf<String?>(null) }
    var updateErrorMessage by remember { mutableStateOf<String?>(null) }
    var updateInfoToShow by remember { mutableStateOf<UpdateInfo?>(null) }

    // Permissions check
    val hasStoragePermission = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val isEn = appLanguage == "en"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isEn) "App Settings & Management" else "অ্যাপ সেটিংস ও ব্যবস্থাপনা",
                    fontFamily = HeadingFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEn) "App update, theme, language & configurations" else "অ্যাপ আপডেট, থিম, ভাষা ও অন্যান্য কনফিগারেশন",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Quick access: Memo Header Settings (centre name, subtitle, quick-preset items)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMemoSettings() }
                .testTag("open_memo_settings_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEn) "Memo Header Settings" else "মেমো হেডার সেটিংস",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEn) "Business name, subtitle, quick-preset items"
                        else "প্রতিষ্ঠানের নাম, সাব-টাইটেল, দ্রুত আইটেম তালিকা",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. App Update Section
        SettingsCard(
            title = if (isEn) "App Updates" else "অ্যাপ আপডেট (App Updates)",
            icon = Icons.Default.SystemUpdate
        ) {
            Text(
                text = if (isEn)
                    "Current Version: v${BuildConfig.VERSION_NAME} (Build: ${BuildConfig.VERSION_CODE})"
                else
                    "বর্তমান ভার্সন: v${BengaliUtils.toBengaliDigits(BuildConfig.VERSION_NAME)} (বিল্ড কোড: ${BengaliUtils.toBengaliDigits(BuildConfig.VERSION_CODE.toString())})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (noUpdateMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = noUpdateMessage!!,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (updateErrorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = updateErrorMessage!!,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFC62828)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (!isCheckingUpdate) {
                        isCheckingUpdate = true
                        noUpdateMessage = null
                        updateErrorMessage = null
                        coroutineScope.launch {
                            val info = updateManager.checkForUpdate(context)
                            isCheckingUpdate = false
                            if (info.errorMessage != null) {
                                updateErrorMessage = info.errorMessage
                            } else if (info.hasUpdate) {
                                updateInfoToShow = info
                            } else {
                                noUpdateMessage = if (isEn) "You are using the latest version." else "আপনি সর্বশেষ সংস্করণ ব্যবহার করছেন।"
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("global_check_updates_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEn) "Checking updates..." else "আপডেট চেক করা হচ্ছে...", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Check for Updates" else "আপডেট চেক করুন", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Theme & Color Palette Section
        SettingsCard(
            title = if (isEn) "App Theme & Color" else "অ্যাপ থিম ও কালার (Theme & Color)",
            icon = Icons.Default.ColorLens
        ) {
            Text(
                text = if (isEn) "Display Mode:" else "ডিসপ্লে মোড:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            val themes = listOf(
                "system" to if (isEn) "System" else "সিস্টেম",
                "light" to if (isEn) "Light" else "লাইট",
                "dark" to if (isEn) "Dark" else "ডার্ক"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { (key, label) ->
                    val isSelected = (themeMode == key)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onThemeModeChange(key) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isEn) "Select Theme Color:" else "থিমের কালার প্যালেট নির্বাচন করুন:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePalettes.chunked(2).forEach { rowPalettes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPalettes.forEach { palette ->
                            val isSelected = (themeColor == palette.id)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) palette.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .background(
                                        if (isSelected) palette.container.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { onThemeColorChange(palette.id) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(palette.previewGradient)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEn) palette.nameEn else palette.nameBn,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) palette.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Language Settings Section
        SettingsCard(
            title = if (isEn) "Language" else "ভাষা (Language)",
            icon = Icons.Default.Language
        ) {
            val languages = listOf(
                "bn" to if (isEn) "Bangla (বাংলা)" else "বাংলা (Bangla) - ডিফল্ট",
                "en" to if (isEn) "English (Active)" else "English (ইংরেজি) - সক্রিয়"
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                languages.forEach { (key, label) ->
                    val isSelected = (appLanguage == key)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLanguageChange(key) }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onLanguageChange(key) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3.5 Font Size Settings — larger touch-friendly text for users who need it
        SettingsCard(
            title = if (isEn) "Text Size" else "লেখার আকার (Text Size)",
            icon = Icons.Default.FormatSize
        ) {
            val sizes = listOf(
                0.9f to (if (isEn) "Small" else "ছোট"),
                1.0f to (if (isEn) "Normal (Default)" else "স্বাভাবিক (ডিফল্ট)"),
                1.15f to (if (isEn) "Large" else "বড়"),
                1.3f to (if (isEn) "Extra Large" else "অতিরিক্ত বড়")
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sizes.forEach { (scale, label) ->
                    val isSelected = kotlin.math.abs(fontScale - scale) < 0.01f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onFontScaleChange(scale) }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onFontScaleChange(scale) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = (13.5f * scale).sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Privacy & Permissions
        SettingsCard(
            title = "প্রাইভেসি ও সিস্টেম পারমিশনস",
            icon = Icons.Default.Security
        ) {
            // Storage Permission Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("স্টোরেজ/ফটোস পারমিশন:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Text(
                    text = if (hasStoragePermission) "অনুমতি দেওয়া হয়েছে ✓" else "স্বয়ংক্রিয়/প্রয়োজন নেই",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("ডিভাইস অ্যাপ সেটিংস ওপেন করুন", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Unified Data Download & Input (Backup & Restore)
        SettingsCard(
            title = if (isEn) "Data Download & Input (Backup & Restore)" else "ডাটা ডাউনলোড ও ইনপুট (ব্যাকআপ ও রিস্টোর)",
            icon = Icons.Default.CloudDownload
        ) {
            Text(
                text = if (isEn) {
                    "Download all app records in a single file and restore them anytime. No need for separate options—all food bills, advance salary records, presets, and preferences are placed into their respective places automatically."
                } else {
                    "অ্যাপের সমস্ত তৈরি করা ডাটা এক ক্লিকে ডাউনলোড ও পরবর্তীতে ইনপুট করুন। আলাদা আলাদা অপশনের প্রয়োজন নেই—মেমো, স্যালারি আবেদন, প্রিসেট ও সেটিংস স্বয়ংক্রিয়ভাবে নিজের জায়গায় প্লেসমেন্ট হয়ে যাবে।"
                },
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option 1: Data Download / Export
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEn) "1. Download All App Data" else "১. ডাটা ব্যাকআপ ডাউনলোড (Export)",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEn) "Save all bills, salary applications, and presets in JSON format" else "সকল মেমো হিসাব, স্যালারি রিকুইজিশন ও প্রিসেট ফাইল ডাউনলোড করুন",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExporting = true
                                    try {
                                        val json = AppBackupManager.exportAllData(context)
                                        cachedJsonToExport = json
                                        val saveMsg = AppBackupManager.saveBackupToDownloads(context, json)
                                        exportSuccessMessage = if (isEn) "Data downloaded successfully! ($saveMsg)" else "ডাটা ব্যাকআপ ডাউনলোড সম্পন্ন! ($saveMsg)"
                                        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                                        createDocumentLauncher.launch("digital_tools_backup_$timeStamp.json")
                                    } catch (e: Exception) {
                                        exportErrorMessage = e.localizedMessage
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("download_all_data_button")
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isEn) "Downloading..." else "ডাউনলোড হচ্ছে...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEn) "Download Data" else "ডাটা ডাউনলোড করুন",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val json = cachedJsonToExport ?: AppBackupManager.exportAllData(context)
                                        AppBackupManager.shareBackupFile(context, json)
                                    } catch (e: Exception) {
                                        exportErrorMessage = e.localizedMessage
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEn) "Share" else "শেয়ার", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (exportSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ $exportSuccessMessage",
                            fontSize = 11.5.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (exportErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✕ $exportErrorMessage",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Data Input / Restore
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEn) "2. Input / Restore Data" else "২. ডাটা ইনপুট / রিস্টোর (Import)",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEn) "Restore JSON file — placed automatically into appropriate sections" else "ডাউনলোডকৃত ব্যাকআপ ফাইল নির্বাচন করুন — অটোমেটিক প্লেসমেন্ট হবে",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        enabled = !isImporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_all_data_button")
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEn) "Importing & Placing data..." else "ডাটা ইনপুট ও প্লেসমেন্ট হচ্ছে...", fontSize = 12.5.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Select Backup File to Input" else "ব্যাকআপ ফাইল নির্বাচন করে ইনপুট দিন",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. About App Section
        SettingsCard(
            title = "অ্যাপ সম্পর্কে (About App)",
            icon = Icons.Default.Info
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ডিজিটাল টুল (Digital Tool)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "আপনার দৈনন্দিন মেমো তৈরি, খাবার বাজার হিসাব সংরক্ষণ এবং NID বা আইডি কার্ড দ্রুত স্ক্যান ও ফিল্টার করার জন্য একটি স্মার্ট বাংলা টুলস অ্যাপ।",
                    fontSize = 12.5.sp,
                    color = Color.DarkGray,
                    lineHeight = 17.sp
                )

                Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE0E0E0))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("অ্যাপ ভার্সন:", fontSize = 12.5.sp, color = Color.Gray)
                    Text("v${BuildConfig.VERSION_NAME}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("বিল্ড কোড:", fontSize = 12.5.sp, color = Color.Gray)
                    Text("${BuildConfig.VERSION_CODE}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("বিল্ড স্ট্যাটাস:", fontSize = 12.5.sp, color = Color.Gray)
                    Text("অফিশিয়াল স্টেবল রিলিজ", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. Developer Info
        SettingsCard(
            title = "ডেভেলপার তথ্য (Developer Info)",
            icon = Icons.Default.DeveloperMode
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ডিজিটাল টুলস ডেভেলপমেন্ট টিম",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "গিটহাব রিপোজিটরি: github.com/nahid6714/tools",
                    fontSize = 12.5.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "যেকোনো মতামত বা পরামর্শের জন্য যোগাযোগ করতে পারেন।",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 8. Open Source Licenses
        SettingsCard(
            title = "ওপেন সোর্স লাইসেন্স",
            icon = Icons.Default.Code
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "• Jetpack Compose & Material 3 (Apache 2.0)",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "• Google ML Kit Document Scanner API",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "• Room Persistence Library (AndroidX)",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Update Dialog if new release detected
    if (updateInfoToShow != null) {
        UpdateDialog(
            updateInfo = updateInfoToShow!!,
            updateManager = updateManager,
            onDismiss = { updateInfoToShow = null }
        )
    }

    // Import Result Dialog
    if (importResultToShow != null) {
        val res = importResultToShow!!
        AlertDialog(
            onDismissRequest = { importResultToShow = null },
            icon = {
                Icon(
                    imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (res.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (res.success) (if (isEn) "Data Imported Successfully!" else "ডাটা ইনপুট সফল হয়েছে!") else (if (isEn) "Import Failed" else "ইনপুট ব্যর্থ হয়েছে"),
                    fontFamily = HeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = res.message,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (res.success) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEn) "✓ All records placed in their respective sections accurately." else "✓ সকল রেকর্ডস ও প্রিসেট নিজ নিজ জায়গায় সঠিকভাবে প্লেসমেন্ট হয়েছে।",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { importResultToShow = null }) {
                    Text(if (isEn) "OK" else "ঠিক আছে")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = HeadingFontFamily
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}
