package com.rk.extension

import androidx.compose.runtime.Composable
import com.rk.drawer.DrawerTab
import com.rk.drawer.ServiceTab
import com.rk.feature.FeatureRegistry
import com.rk.icons.Icon
import com.rk.resources.drawables
import com.rk.resources.getString
import com.rk.resources.strings
import com.rk.settings.extension.PluginsDrawerPanel

class PluginTab : ServiceTab() {
    override fun getName(): String {
        return strings.ext.getString()
    }

    override fun getIcon(): Icon {
        return Icon.ResourceIcon(drawables.extension)
    }

    override fun isSupported(drawerTab: DrawerTab?): Boolean {
        return FeatureRegistry.isEnabled("enable_extension")
    }

    @Composable
    override fun Content(drawerTab: DrawerTab?) {
        PluginsDrawerPanel()
    }
}
