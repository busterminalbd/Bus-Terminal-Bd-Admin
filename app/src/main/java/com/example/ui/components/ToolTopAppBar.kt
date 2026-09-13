package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * THE shared tool-screen header. Every tool (Food Bill Memo, Advance Salary
 * Application, Medical Work Report, and any tool added later) must use this
 * composable for its top app bar instead of building its own TopAppBar.
 *
 * What this locks in as "same everywhere":
 *  - background color -> MaterialTheme.colorScheme.primary (never a hardcoded
 *    color, so it follows the user's chosen theme color)
 *  - white title/icon color
 *  - a back button on the left (when [onNavigateBack] is supplied)
 *  - a global-settings gear on the far right, wired to [onOpenGlobalSettings],
 *    with the shared testTag "global_settings_button" so it's easy to find
 *    in UI tests regardless of which tool screen it's on
 *
 * What each tool still controls:
 *  - [title] — the tool's own title/subtitle content
 *  - [extraActions] — any tool-specific action icons (e.g. a "⋮" menu, a
 *    reset button), rendered just to the left of the settings gear
 *
 * If a tool screen needs something this doesn't support, extend this
 * composable rather than writing a second TopAppBar from scratch — that's
 * how the header design silently drifts apart between tools.
 */
@Composable
fun ToolTopAppBar(
    title: @Composable () -> Unit,
    onOpenGlobalSettings: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    extraActions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = Color.White
                    )
                }
            }
        },
        actions = {
            extraActions()
            IconButton(
                onClick = onOpenGlobalSettings,
                modifier = Modifier
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "মেইন অ্যাপ সেটিংস",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White
        )
    )
}
