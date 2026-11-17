package com.rk.settings.extension

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rk.extension.InstallState
import com.rk.icons.Download
import com.rk.icons.XedIcons
import com.rk.resources.drawables
import com.rk.resources.strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SmallExtensionActionButton(
    installState: InstallState,
    scope: CoroutineScope,
    onInstallClick: suspend () -> Unit,
    onUninstallClick: suspend () -> Unit,
    onUpdateClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    outdatedWarning: Boolean = false,
) {
    if (outdatedWarning) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = stringResource(strings.warning),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(8.dp),
        )
        return
    }

    when (installState) {
        InstallState.Idle -> {
            Box(
                modifier =
                    modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                        .clickable { scope.launch { onInstallClick() } }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = XedIcons.Download,
                        contentDescription = stringResource(strings.install),
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF2563EB),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(strings.install),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2563EB),
                    )
                }
            }
        }

        InstallState.Installing -> {
            Box(
                modifier =
                    modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2563EB),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(strings.installing),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2563EB),
                    )
                }
            }
        }

        InstallState.Installed -> {
            Box(
                modifier =
                    modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                        .clickable { scope.launch { onUninstallClick() } }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(strings.delete),
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(strings.delete),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        InstallState.Updatable -> {
            Box(
                modifier =
                    modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.14f))
                        .clickable { scope.launch { onUpdateClick() } }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(drawables.update),
                        contentDescription = stringResource(strings.update),
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF10B981),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(strings.update),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981),
                    )
                }
            }
        }

        InstallState.Updating -> {
            Box(
                modifier =
                    modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF10B981),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(strings.updating),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981),
                    )
                }
            }
        }
    }
}

@Composable
fun ExtensionActionButtons(
    installState: InstallState,
    scope: CoroutineScope,
    onInstallClick: suspend () -> Unit,
    onUninstallClick: suspend () -> Unit,
    onUpdateClick: suspend () -> Unit,
    onRecommendedClick: () -> Unit,
    outdatedWarning: Boolean = false,
    showRecommendedButton: Boolean = false,
    progress: Float,
) {

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (installState) {
            InstallState.Idle -> {
                InstallButton(scope, onInstallClick, Modifier.weight(1f), outdatedWarning)
            }

            InstallState.Installing -> {
                InstallingButton(Modifier.weight(1f), progress)
            }

            InstallState.Installed -> {
                UninstallButton(scope, onUninstallClick, Modifier.weight(1f))
            }

            InstallState.Updatable -> {
                UpdateButton(scope, onUpdateClick, Modifier.weight(1f), outdatedWarning)
                UninstallButton(scope, onUninstallClick, Modifier.weight(1f))
            }

            InstallState.Updating -> {
                UpdatingButton(Modifier.weight(1f), progress)
            }
        }

        if (showRecommendedButton) {
            RecommendationsButton(Modifier, onRecommendedClick)
        }
    }
}

@Composable
private fun RecommendationsButton(
    modifier: Modifier = Modifier,
    onRecommendedClick: () -> Unit,
) {
    FilledIconButton(
        modifier = modifier,
        onClick = onRecommendedClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(painterResource(drawables.widgets), contentDescription = stringResource(strings.recommendations))
    }
}

@Composable
private fun InstallButton(
    scope: CoroutineScope,
    onInstallClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    outdatedWarning: Boolean = false,
) {
    Button(modifier = modifier, enabled = !outdatedWarning, onClick = { scope.launch { onInstallClick() } }) {
        Icon(XedIcons.Download, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(strings.install))
    }
}

@Composable
private fun InstallingButton(modifier: Modifier = Modifier, progress: Float) {
    val shape = ButtonDefaults.shape

    Button(
        modifier = modifier,
        onClick = {},
        enabled = false,
        contentPadding = PaddingValues(0.dp),
        shape = shape,
    ) {
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        Box(
            modifier =
                Modifier.heightIn(min = ButtonDefaults.MinHeight).fillMaxWidth().clip(shape).drawBehind {
                    drawRect(
                        color = primaryContainer,
                        size =
                            Size(
                                width = size.width * progress.coerceIn(0f, 1f),
                                height = size.height,
                            ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )

                Spacer(Modifier.width(8.dp))

                Text("${stringResource(strings.installing)} (${(progress * 100).toInt()}%)")
            }
        }
    }
}

@Composable
private fun UninstallButton(
    scope: CoroutineScope,
    onUninstallClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = { scope.launch { onUninstallClick() } },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = stringResource(strings.delete), Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(strings.uninstall))
    }
}

@Composable
private fun UpdateButton(
    scope: CoroutineScope,
    onUpdateClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    outdatedWarning: Boolean = false,
) {
    Button(
        modifier = modifier,
        enabled = !outdatedWarning,
        onClick = { scope.launch { onUpdateClick() } },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
    ) {
        Icon(
            painterResource(drawables.update),
            contentDescription = stringResource(strings.update),
            Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(stringResource(strings.update))
    }
}

@Composable
private fun UpdatingButton(
    modifier: Modifier = Modifier,
    progress: Float,
) {
    val shape = ButtonDefaults.shape

    Button(
        modifier = modifier,
        onClick = {},
        enabled = false,
        contentPadding = PaddingValues(0.dp),
        shape = shape,
    ) {
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        Box(
            modifier =
                Modifier.heightIn(min = ButtonDefaults.MinHeight).fillMaxWidth().clip(shape).drawBehind {
                    drawRect(
                        color = primaryContainer,
                        size =
                            Size(
                                width = size.width * progress.coerceIn(0f, 1f),
                                height = size.height,
                            ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )

                Spacer(Modifier.width(8.dp))

                Text("${stringResource(strings.updating)} (${(progress * 100).toInt()}%)")
            }
        }
    }
}
