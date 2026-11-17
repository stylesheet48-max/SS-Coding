package com.rk.account

object AccountConfig {
    // Owner Controlled Endpoint (Default to empty in Owner Local/Offline Mode)
    const val WEB_BASE = ""

    const val API_BASE = ""

    const val WEB_CALLBACK_PATH = "/android/auth/callback"

    const val WEB_CALLBACK_URL = ""

    const val DEEP_LINK_SCHEME = "sscoding"

    const val DEEP_LINK_HOST = "auth"

    const val DEEP_LINK_PATH = "/callback"

    const val DEEP_LINK_URL = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST$DEEP_LINK_PATH"

    const val WEB_HOST = ""

    const val DASHBOARD_URL = ""
}
