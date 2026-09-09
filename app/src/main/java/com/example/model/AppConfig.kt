package com.example.model

import androidx.compose.runtime.Stable

@Stable
data class AppConfig(
    val enableSplitTunneling: Boolean = true,
    val excludedApps: Set<String> = EXCLUDED_APPS_DEFAULT,
    val enablePacketCapture: Boolean = false,
    val captureMode: CaptureMode = CaptureMode.SELECTIVE,
    val maxPacketsBuffer: Int = 10000
)

enum class CaptureMode {
    FULL,      // Capture all traffic (system-wide)
    SELECTIVE, // Capture only from selected apps (FIXED: This is the default)
    EXCLUDED   // Capture all except excluded apps
}

val EXCLUDED_APPS_DEFAULT = setOf(
    "com.twitter.android",           // X (Twitter)
    "com.google.android.youtube",    // YouTube
    "com.instagram.android",         // Instagram
    "com.facebook.katana",           // Facebook
    "com.whatsapp",                  // WhatsApp
    "com.google.android.gms",        // Google Play Services
    "com.android.chrome",            // Chrome
    "com.opera.browser",             // Opera
    "com.UCMobile.intl",             // UC Browser
    "com.google.android.apps.maps",  // Google Maps
    "com.spotify.music",             // Spotify
    "com.netflix.mediaclient"        // Netflix
)
