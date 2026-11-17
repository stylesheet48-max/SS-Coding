package com.rk.drawer

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.compose.dnd.reorder.ReorderContainer
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItem
import com.mohamedrejeb.compose.dnd.reorder.rememberReorderState
import com.rk.activities.main.MainActivity
import com.rk.components.DrawerRailWidth
import com.rk.file.toFileObject
import com.rk.filetree.ProjectCloseConfirmationDialog
import com.rk.icons.XedIcon
import com.rk.resources.drawables
import com.rk.resources.getString
import com.rk.resources.strings
import com.rk.utils.dialogRes
import com.rk.utils.logError
import kotlinx.coroutines.launch

private fun validateValue(value: String): String? {
    return when {
        value.isBlank() -> strings.value_empty_err.getString()
        else -> null
    }
}

@Composable
fun DrawerContent(fullscreen: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mainActivity = LocalActivity.current as MainActivity
    val viewModel = mainActivity.drawerViewModel

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val drawerTabs by viewModel.drawerTabs.collectAsStateWithLifecycle()
    val serviceTabs by viewModel.serviceTabs.collectAsStateWithLifecycle()
    val currentDrawerTabIndex by viewModel.currentDrawerTabIndex.collectAsStateWithLifecycle()
    val currentServiceTabIndex by viewModel.currentServiceTabIndex.collectAsStateWithLifecycle()
    val currentDrawerTab = drawerTabs.getOrNull(currentDrawerTabIndex)
    val currentServiceTab = serviceTabs.getOrNull(currentServiceTabIndex)

    val openFolder =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
            onResult = { uri ->
                uri?.let {
                    runCatching {
                        // Persist access permissions (required for Android 5.0+)
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                        .onFailure { logError(it) }

                    scope.launch { viewModel.addFileTreeTab(it.toFileObject(expectedIsFile = false)) }
                }
            },
        )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxSize()) {
                val scope = rememberCoroutineScope()
                var showAddDialog by rememberSaveable { mutableStateOf(false) }
                var closeProjectDialog by remember { mutableStateOf(false) }

                val lazyListState = rememberLazyListState()
                val showHorizontalDivider by remember { derivedStateOf { lazyListState.canScrollForward } }

                NavigationRail(
                    modifier = Modifier.width(DrawerRailWidth),
                    windowInsets = if (fullscreen) WindowInsets() else NavigationRailDefaults.windowInsets,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    val reorderState = rememberReorderState<DrawerTab>(dragAfterLongPress = true)

                    ReorderContainer(state = reorderState) {
                        Column(modifier = Modifier.fillMaxHeight()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f, fill = true),
                                state = lazyListState,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(items = drawerTabs, key = { it.hashCode() }) { tab ->
                                    if (!tab.isSupported()) return@items

                                    val isTabSelected = currentDrawerTab == tab
                                    val isDimmed = currentServiceTab != null
                                    val projectAccent = Color(0xFFF59E0B) // Amber Gold

                                    ReorderableItem(
                                        state = reorderState,
                                        key = tab.hashCode(),
                                        data = tab,
                                        onDragEnter = { state ->
                                            val fromIndex = drawerTabs.indexOf(state.data)
                                            val toIndex = drawerTabs.indexOf(tab)
                                            if (fromIndex != -1 && toIndex != -1) {
                                                viewModel.moveDrawerTab(fromIndex, toIndex)
                                            }
                                        },
                                        draggableContent = {
                                            DrawerRailItem(
                                                modifier = Modifier.alpha(0.6f),
                                                selected = isTabSelected,
                                                isDimmed = isDimmed,
                                                accentColor = projectAccent,
                                                icon = { XedIcon(tab.getIcon()) },
                                                label = tab.getName(),
                                                onClick = {
                                                    if (currentDrawerTab == tab && currentServiceTab == null) {
                                                        closeProjectDialog = true
                                                    } else {
                                                        viewModel.selectDrawerTab(tab)
                                                    }
                                                },
                                                enabled = tab.isEnabled(),
                                            )
                                        },
                                    ) {
                                        DrawerRailItem(
                                            selected = isTabSelected,
                                            isDimmed = isDimmed,
                                            accentColor = projectAccent,
                                            icon = { XedIcon(tab.getIcon()) },
                                            label = tab.getName(),
                                            onClick = {
                                                if (currentDrawerTab == tab && currentServiceTab == null) {
                                                    closeProjectDialog = true
                                                } else {
                                                    viewModel.selectDrawerTab(tab)
                                                }
                                            },
                                            enabled = tab.isEnabled(),
                                        )
                                    }
                                }

                                item {
                                    DrawerRailItem(
                                        selected = false,
                                        accentColor = Color(0xFF2563EB), // Royal Blue
                                        icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
                                        label = stringResource(strings.add),
                                        onClick = { showAddDialog = true },
                                    )
                                }
                            }

                            if (showHorizontalDivider)
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )

                            Column(
                                modifier = Modifier.wrapContentHeight().padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                serviceTabs.forEach { tab ->
                                    if (!tab.isSupported(currentDrawerTab)) return@forEach
                                    val isTabSelected = currentServiceTab == tab
                                    val accentColor =
                                        when {
                                            tab.getName().equals(strings.git.getString(), ignoreCase = true) ||
                                                tab.getName().contains("git", ignoreCase = true) -> Color(0xFFF97316)
                                            tab.getName().equals(strings.ext.getString(), ignoreCase = true) ||
                                                tab.getName().contains("plugin", ignoreCase = true) ||
                                                tab.getName().contains("ext", ignoreCase = true) -> Color(0xFF7C3AED)
                                            else -> Color(0xFF0D9488)
                                        }

                                    DrawerRailItem(
                                        selected = isTabSelected,
                                        accentColor = accentColor,
                                        icon = { XedIcon(icon = tab.getIcon()) },
                                        label = tab.getName(),
                                        onClick = {
                                            if (currentServiceTab == tab) {
                                                viewModel.unselectServiceTab()
                                            } else {
                                                viewModel.selectServiceTab(tab)
                                            }
                                        },
                                        enabled = tab.isEnabled(currentDrawerTab),
                                    )
                                }
                            }
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Surface {
                    Crossfade(targetState = currentDrawerTab, label = "file tree") { tab ->
                        if (currentServiceTab == null) {
                            if (tab != null) {
                                tab.Content()
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        painter = painterResource(drawables.outline_folder),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(strings.no_folder_opened),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    Crossfade(targetState = currentServiceTab) { tab -> tab?.Content(currentDrawerTab) }
                }

                if (showAddDialog) {
                    AddProjectSheet(
                        onDismiss = { showAddDialog = false },
                        openFolder = openFolder,
                        onAddProject = { fileObject -> scope.launch { viewModel.addFileTreeTab(fileObject, true) } },
                        showPrivateFileWarning = { callback ->
                            dialogRes(
                                title = strings.attention.getString(),
                                msg = strings.warning_private_dir.getString(),
                                onOk = { callback.invoke() },
                            )
                        },
                    )
                }

                if (closeProjectDialog && currentDrawerTab != null) {
                    ProjectCloseConfirmationDialog(
                        projectName = currentDrawerTab.getName(),
                        onConfirm = {
                            closeProjectDialog = false
                            viewModel.removeDrawerTab(currentDrawerTab)
                        },
                        onDismiss = { closeProjectDialog = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerRailItem(
    selected: Boolean,
    accentColor: Color,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDimmed: Boolean = false,
) {
    val isActuallySelected = selected && !isDimmed
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    val containerBg =
        when {
            isActuallySelected -> accentColor.copy(alpha = if (isLight) 0.16f else 0.24f)
            selected && isDimmed -> accentColor.copy(alpha = if (isLight) 0.08f else 0.12f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLight) 0.35f else 0.18f)
        }

    val borderModifier =
        when {
            isActuallySelected ->
                Modifier.border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(13.dp),
                )
            selected && isDimmed ->
                Modifier.border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(13.dp),
                )
            else -> Modifier
        }

    val iconTint =
        when {
            isActuallySelected -> accentColor
            selected && isDimmed -> accentColor.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    val textColor =
        when {
            isActuallySelected -> accentColor
            selected && isDimmed -> accentColor.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (isActuallySelected) {
            Box(
                modifier =
                    Modifier.align(Alignment.CenterStart)
                        .width(3.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                        .background(accentColor)
            )
        }

        NavigationRailItem(
            modifier = Modifier.padding(horizontal = 4.dp),
            selected = isActuallySelected,
            onClick = onClick,
            enabled = enabled,
            icon = {
                Box(
                    modifier =
                        Modifier.size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(containerBg)
                            .then(borderModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides iconTint) { icon() }
                }
            },
            label = {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isActuallySelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 10.5.sp,
                    color = textColor,
                )
            },
            colors =
                NavigationRailItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = accentColor,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = textColor,
                    unselectedTextColor = textColor,
                ),
        )
    }
}
