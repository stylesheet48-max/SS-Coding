package com.rk.settings.extension

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rk.App.Companion.iconPackManager
import com.rk.App.Companion.themeManager
import com.rk.activities.settings.SettingsActivity
import com.rk.activities.settings.SettingsRoutes
import com.rk.common.PackageType
import com.rk.components.DrawerToolbar
import com.rk.extension.extensionManager
import com.rk.extension.model.Package
import com.rk.resources.drawables
import com.rk.resources.strings
import com.rk.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsDrawerPanel() {
    val context = LocalContext.current
    val activity = LocalActivity.current as? AppCompatActivity
    val scope = rememberCoroutineScope()

    var refreshKey by remember { mutableIntStateOf(0) }
    var selectedCategory by rememberSaveable { mutableStateOf(StoreCategory.EXTENSIONS) }
    var currentSortOption by remember { mutableStateOf(ExtensionSortOptions.DOWNLOAD_COUNT) }
    var currentFilterOption by remember { mutableStateOf(ExtensionFilterOptions.ALL) }
    val searchQuery = rememberTextFieldState("")
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val dialogManager = remember { ExtensionDialogManager() }

    val installedExtensions by extensionManager.installedExtensions.collectAsStateWithLifecycle()
    val storeExtensions by extensionManager.storeExtension.collectAsStateWithLifecycle()
    val localThemes by themeManager.localThemes.collectAsStateWithLifecycle()
    val storeThemes by themeManager.storeThemes.collectAsStateWithLifecycle()
    val localIconPacks by iconPackManager.localIconPacks.collectAsStateWithLifecycle()
    val storeIconPacks by iconPackManager.storeIconPacks.collectAsStateWithLifecycle()

    var isIndexing by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey, selectedCategory) {
        val isForceRefresh = refreshKey > 0

        val needsLocal =
            when (selectedCategory) {
                StoreCategory.EXTENSIONS -> isForceRefresh || extensionManager.installedExtensions.value.isEmpty()
                StoreCategory.THEMES -> isForceRefresh || themeManager.localThemes.value.isEmpty()
                StoreCategory.ICON_PACKS -> isForceRefresh || iconPackManager.localIconPacks.value.isEmpty()
            }

        val needsStore =
            when (selectedCategory) {
                StoreCategory.EXTENSIONS -> isForceRefresh || extensionManager.storeExtension.value.isEmpty()
                StoreCategory.THEMES -> isForceRefresh || themeManager.storeThemes.value.isEmpty()
                StoreCategory.ICON_PACKS -> isForceRefresh || iconPackManager.storeIconPacks.value.isEmpty()
            }

        if (needsLocal || needsStore) {
            isIndexing = needsLocal
            isFetching = needsStore

            val localJob =
                if (needsLocal) {
                    launch(Dispatchers.IO) {
                        runCatching {
                            when (selectedCategory) {
                                StoreCategory.EXTENSIONS -> extensionManager.indexLocalExtensions()
                                StoreCategory.THEMES -> themeManager.indexLocalThemes()
                                StoreCategory.ICON_PACKS -> iconPackManager.indexLocalPacks()
                            }
                        }
                        isIndexing = false
                    }
                } else null

            val storeJob =
                if (needsStore) {
                    launch(Dispatchers.IO) {
                        runCatching {
                            when (selectedCategory) {
                                StoreCategory.EXTENSIONS -> extensionManager.indexStoreExtensions()
                                StoreCategory.THEMES -> themeManager.indexStoreThemes()
                                StoreCategory.ICON_PACKS -> iconPackManager.indexStoreIconPacks()
                            }
                        }
                        isFetching = false
                    }
                } else null

            localJob?.join()
            storeJob?.join()
        }
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
            installAutoDetect(scope, uri, activity, dialogManager.asScanApproval())
        }

    val extensions by
        remember(installedExtensions, storeExtensions, searchQuery.text, currentFilterOption, currentSortOption) {
            derivedStateOf {
                val all = extensionManager.getSyncedExtensions()
                val filtered = applyExtensionsFilter(searchQuery, all, currentFilterOption)
                applyExtensionsSort(currentSortOption, filtered)
            }
        }

    val sortedThemes by
        remember(localThemes, storeThemes, searchQuery.text) {
            derivedStateOf {
                val all = themeManager.getSyncedThemes()
                val filtered = applyGenericFilter(searchQuery, all)
                filtered.sortedBy { it.name }
            }
        }

    val sortedIconPacks by
        remember(localIconPacks, storeIconPacks, searchQuery.text) {
            derivedStateOf {
                val all = iconPackManager.getSyncedIconPacks()
                val filtered = applyGenericFilter(searchQuery, all)
                filtered.sortedBy { it.name }
            }
        }

    val currentPackages: List<Package> =
        when (selectedCategory) {
            StoreCategory.EXTENSIONS -> extensions
            StoreCategory.THEMES -> sortedThemes
            StoreCategory.ICON_PACKS -> sortedIconPacks
        }

    Column(modifier = Modifier.fillMaxSize()) {
        ExtensionDialogRenderer(dialogManager)

        // Panel Toolbar
        DrawerToolbar(
            isLoading = isIndexing || isFetching,
            left = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier.size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF7C3AED).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(drawables.extension),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF7C3AED),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(strings.ext),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier =
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF7C3AED).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentPackages.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7C3AED),
                        )
                    }
                }
            },
            right = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    // Search toggle button
                    IconButton(
                        onClick = {
                            isSearchOpen = !isSearchOpen
                            if (!isSearchOpen) searchQuery.clearText()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = stringResource(strings.search),
                            modifier = Modifier.size(20.dp),
                            tint = if (isSearchOpen) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Install from storage (+)
                    IconButton(
                        onClick = {
                            if (Settings.warn_extensions) {
                                dialogManager.showWarning { filePickerLauncher.launch(arrayOf("*/*")) }
                            } else {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            }
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(strings.install_from_storage),
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF2563EB),
                        )
                    }

                    // Filter & Sort Menu
                    Box {
                        IconButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                painter = painterResource(drawables.filter),
                                contentDescription = "Filter",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                        ) {
                            Text(
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp, end = 16.dp),
                                text = "Filter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF7C3AED),
                            )
                            ExtensionFilterOptions.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(opt.stringRes), fontSize = 13.sp) },
                                    leadingIcon = {
                                        RadioButton(selected = currentFilterOption == opt, onClick = null)
                                    },
                                    onClick = {
                                        currentFilterOption = opt
                                        showFilterMenu = false
                                    },
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 16.dp),
                                text = stringResource(strings.sort_options),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF7C3AED),
                            )
                            ExtensionSortOptions.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(opt.stringRes), fontSize = 13.sp) },
                                    leadingIcon = { RadioButton(selected = currentSortOption == opt, onClick = null) },
                                    onClick = {
                                        currentSortOption = opt
                                        showFilterMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )

        // Expandable Search Bar
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                SearchBarDefaults.InputField(
                    modifier = Modifier.fillMaxWidth(),
                    state = searchQuery,
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.text.isNotEmpty()) {
                            IconButton(onClick = { searchQuery.clearText() }) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = {
                        Text(
                            when (selectedCategory) {
                                StoreCategory.EXTENSIONS -> stringResource(strings.search)
                                StoreCategory.THEMES -> stringResource(strings.search_themes)
                                StoreCategory.ICON_PACKS -> stringResource(strings.search_icon_packs)
                            },
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        // Category Pills
        ScrollableTabRow(
            selectedTabIndex = selectedCategory.ordinal,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                    color = Color(0xFF7C3AED),
                    height = 2.5.dp,
                )
            },
            divider = {},
        ) {
            StoreCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                Tab(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(category.drawableRes),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint =
                                    if (isSelected) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(category.stringRes),
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color =
                                    if (isSelected) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // Package List
        if (currentPackages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isIndexing || isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF7C3AED))
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(selectedCategory.drawableRes),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =
                                when (selectedCategory) {
                                    StoreCategory.EXTENSIONS -> stringResource(strings.no_ext)
                                    StoreCategory.THEMES -> stringResource(strings.no_themes)
                                    StoreCategory.ICON_PACKS -> stringResource(strings.no_icon_packs)
                                },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(items = currentPackages, key = { it.id }) { pkg ->
                    val installState = rememberPackageInstallState(pkg)

                    PackageCard(
                        pkg = pkg,
                        installState = installState,
                        onInstallClick = { runPackageInstallAction(pkg, {}, context, activity, dialogManager) },
                        onUninstallClick = { runPackageUninstallAction(pkg, {}, scope, activity) },
                        onUpdateClick = { runPackageUpdateAction(pkg, {}, context, activity, dialogManager) },
                        onClick = {
                            val route =
                                when (pkg.type) {
                                    PackageType.EXTENSION -> SettingsRoutes.ExtensionDetail.route
                                    PackageType.THEME -> SettingsRoutes.ThemeDetail.route
                                    PackageType.ICON_PACK -> SettingsRoutes.IconPackDetail.route
                                }
                            val intent =
                                Intent(context, SettingsActivity::class.java).apply {
                                    putExtra("route", "$route/${pkg.id}")
                                }
                            context.startActivity(intent)
                        },
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
