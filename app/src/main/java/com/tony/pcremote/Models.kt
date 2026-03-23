package com.tony.pcremote

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val cpu: Float,
    val memory: Long
)

data class FileItem(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val path: String
)

data class VolumeDeviceInfo(
    val id: String,
    val name: String,
    val level: Int,
    val isMuted: Boolean
)
