package com.rk.settings

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rk.account.AccountManager
import com.rk.activities.settings.SettingsRoutes
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.components.compose.preferences.category.PreferenceCategory
import com.rk.feature.FeatureRegistry
import com.rk.icons.XedIcon
import com.rk.resources.drawables
import com.rk.resources.getFilledString
import com.rk.resources.getString
import com.rk.resources.strings
import com.rk.settings.account.ProfileIcon

@Composable
fun SettingsScreen(navController: NavController) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier =
            Modifier.fillMaxSize().background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF5F7FD))
    ) {
        PreferenceLayout(
            label = stringResource(id = strings.settings),
            backArrowVisible = true,
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(id = strings.search),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(id = strings.more),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        ) {
            Categories(navController)
        }
    }
}

@Composable
private fun Categories(navController: NavController) {
    val isDark = isSystemInDarkTheme()
    val blueAccent = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    // Top Hero Banner: "SS coding" + "Configure and personalize your editor experience." + Soft Gear Icon
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors =
                                    if (isDark) {
                                        listOf(Color(0xFF1B243B), Color(0xFF151C30))
                                    } else {
                                        listOf(Color(0xFFEBF1FD), Color(0xFFD6E3FC))
                                    }
                            ),
                        shape = RoundedCornerShape(22.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF283656) else Color(0xFFD6E4FA),
                        shape = RoundedCornerShape(22.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SS coding",
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure and personalize your editor experience.",
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Decorative gear icon
                Box(
                    modifier =
                        Modifier.size(54.dp)
                            .background(
                                color = if (isDark) Color(0xFF26365C) else Color(0xFFC7DBFA),
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = drawables.settings),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isDark) Color(0xFF7EA6F8) else Color(0xFF4A7BD8),
                    )
                }
            }
        }
    }

    // 1. Account
    if (FeatureRegistry.isEnabled("account")) {
        val account by AccountManager.session.collectAsState()
        PreferenceCategory(
            label = stringResource(strings.account),
            description =
                account?.user?.let { user -> user.name?.takeIf { it.isNotBlank() } ?: user.email.ifBlank { null } }
                    ?: stringResource(strings.account_desc),
            startWidget = {
                if (account?.user != null) {
                    ProfileIcon(user = account?.user, avatarSize = 24.dp)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = blueAccent,
                    )
                }
            },
            onNavigate = { navController.navigate(SettingsRoutes.Account.route) },
        )
    }

    // 2. Application
    PreferenceCategory(
        label = stringResource(id = strings.app),
        description = stringResource(id = strings.app_desc),
        iconResource = drawables.android,
        onNavigate = { navController.navigate(SettingsRoutes.AppSettings.route) },
    )

    // 3. Themes
    PreferenceCategory(
        label = stringResource(strings.themes),
        description = stringResource(strings.theme_settings),
        iconResource = drawables.palette,
        onNavigate = { navController.navigate(SettingsRoutes.Themes.route) },
    )

    // 4. Editor
    PreferenceCategory(
        label = stringResource(id = strings.editor),
        description = stringResource(id = strings.editor_desc),
        iconResource = drawables.edit_note,
        onNavigate = { navController.navigate(SettingsRoutes.EditorSettings.route) },
    )

    // 5. Keybindings
    PreferenceCategory(
        label = stringResource(strings.keybindings),
        description = stringResource(strings.keybindings_desc),
        iconResource = drawables.keyboard,
        onNavigate = { navController.navigate(SettingsRoutes.Keybindings.route) },
    )

    // Dynamic Categories sorted to match screenshot: Runners -> Terminal -> Store -> Git -> AI
    val dynamicCategories = SettingsRegistry.categories.collectAsState().value
    val preferredOrder =
        listOf(
            stringResource(id = strings.runners),
            stringResource(id = strings.terminal),
            stringResource(id = strings.store),
            stringResource(id = strings.git),
            stringResource(id = strings.ai_feature_label),
        )
    val sortedCategories = dynamicCategories.sortedBy { category ->
        val idx = preferredOrder.indexOf(category.label)
        if (idx != -1) idx else 99
    }

    sortedCategories.forEach { category ->
        PreferenceCategory(
            label = category.label,
            description = category.description,
            startWidget = {
                XedIcon(
                    icon = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = blueAccent,
                )
            },
            onNavigate = { navController.navigate(category.route) },
        )
    }

    // Debug Options
    if (FeatureRegistry.isEnabled("debug_mode")) {
        PreferenceCategory(
            label = stringResource(strings.debug_options),
            description = strings.debug_options_desc.getFilledString(strings.app_name.getString()),
            iconResource = drawables.build,
            onNavigate = { navController.navigate(SettingsRoutes.DeveloperOptions.route) },
        )
    }

    // About
    PreferenceCategory(
        label = stringResource(id = strings.about),
        description = stringResource(id = strings.about_desc),
        startWidget = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = blueAccent,
            )
        },
        onNavigate = { navController.navigate(SettingsRoutes.About.route) },
    )

    // Support
    PreferenceCategory(
        label = stringResource(strings.support),
        description = stringResource(id = strings.support_desc),
        startWidget = { HeartbeatIcon(blueAccent) },
        onNavigate = { navController.navigate(SettingsRoutes.Support.route) },
    )

    // Bottom Branding Banner Card
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors =
                                    if (isDark) {
                                        listOf(Color(0xFF1B243B), Color(0xFF151C30))
                                    } else {
                                        listOf(Color(0xFFEBF1FD), Color(0xFFD6E3FC))
                                    }
                            ),
                        shape = RoundedCornerShape(22.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF283656) else Color(0xFFD6E4FA),
                        shape = RoundedCornerShape(22.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dark navy/indigo squircle badge with </>
                Box(
                    modifier =
                        Modifier.size(52.dp)
                            .background(
                                color = Color(0xFF1E2548),
                                shape = RoundedCornerShape(16.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "</>",
                        color = Color.White,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            ),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "SS coding",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "v1.0.0 (Release)",
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "com.stylesheet.editor",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
fun HeartbeatIcon(tint: Color = MaterialTheme.colorScheme.primary) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")

    val scale =
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "scale",
        )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).scale(scale.value)) {
        Icon(
            imageVector =
                if (Settings.donated) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = tint,
        )
    }
}
