package com.rk.components.compose.preferences.category

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PreferenceCategory(
    modifier: Modifier = Modifier,
    label: String,
    @DrawableRes iconResource: Int? = null,
    onNavigate: () -> Unit,
    isSelected: Boolean = false,
    description: String? = null,
    endWidget: (@Composable () -> Unit)? = null,
    startWidget: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1B2030) else Color.White
    val borderColor = if (isDark) Color(0xFF262D42) else Color(0xFFE8EEF8)
    val iconContainerBg = if (isDark) Color(0xFF1F2944) else Color(0xFFEEF4FF)
    val iconTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.5.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = enabled) { onNavigate() },
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Soft blue squircle icon container
                Box(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .background(
                                color = iconContainerBg,
                                shape = RoundedCornerShape(13.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides iconTint) {
                        if (iconResource != null) {
                            Icon(
                                painter = painterResource(id = iconResource),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = iconTint,
                            )
                        } else if (startWidget != null) {
                            startWidget()
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.5.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.5.dp))
                        Text(
                            text = description,
                            style =
                                MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Trailing Chevron Navigation Arrow
            if (endWidget != null) {
                endWidget()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}
