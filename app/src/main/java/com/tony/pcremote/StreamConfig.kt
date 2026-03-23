package com.tony.pcremote

data class StreamConfig(
    val width:   Int = 960,
    val height:  Int = 540,
    val fps:     Int = 30,
    val quality: Int = 45
)

// Presets — HIGH and ULTRA require Premium
object StreamPresets {
    val LOW    = StreamConfig(640,   360, 15, 30)
    val MEDIUM = StreamConfig(960,   540, 30, 45)
    val HIGH   = StreamConfig(1280,  720, 30, 60)   // Premium
    val ULTRA  = StreamConfig(1920, 1080, 60, 80)   // Premium

    val all = listOf(
        "Low (360p 15fps)"    to LOW,
        "Medium (540p 30fps)" to MEDIUM,
        "High (720p 30fps) ⭐" to HIGH,
        "Ultra (1080p 60fps) ⭐" to ULTRA
    )
}
