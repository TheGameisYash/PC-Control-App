package com.tony.pcremote

// Windows Virtual Key codes — same values C# VirtualKeyCode enum uses
// This is the shared "language" between Android and C# server
object KeyCodes {
    const val ESC: Short          = 0x1B
    const val F1: Short           = 0x70
    const val F2: Short           = 0x71
    const val F3: Short           = 0x72
    const val F4: Short           = 0x73
    const val F5: Short           = 0x74
    const val F6: Short           = 0x75
    const val F7: Short           = 0x76
    const val F8: Short           = 0x77
    const val F9: Short           = 0x78
    const val F10: Short          = 0x79
    const val F11: Short          = 0x7A
    const val F12: Short          = 0x7B
    const val TAB: Short          = 0x09
    const val BACKSPACE: Short    = 0x08
    const val ENTER: Short        = 0x0D
    const val DELETE: Short       = 0x2E
    const val CTRL: Short         = 0x11
    const val ALT: Short          = 0x12
    const val SHIFT: Short        = 0x10
    const val SPACE: Short        = 0x20
    const val UP: Short           = 0x26
    const val DOWN: Short         = 0x28
    const val LEFT: Short         = 0x25
    const val RIGHT: Short        = 0x27
    const val WIN: Short          = 0x5B
    const val SNAPSHOT: Short     = 0x2C  // PrintScreen
    const val HOME: Short         = 0x24
    const val END: Short          = 0x23
    // Media
    const val VOL_MUTE: Short     = 0xAD.toShort()
    const val VOL_DOWN: Short     = 0xAE.toShort()
    const val VOL_UP: Short       = 0xAF.toShort()
    const val MEDIA_NEXT: Short   = 0xB0.toShort()
    const val MEDIA_PREV: Short   = 0xB1.toShort()
    const val MEDIA_STOP: Short   = 0xB2.toShort()
    const val MEDIA_PLAY: Short   = 0xB3.toShort()
    // Letters (A-Z = 0x41-0x5A, used for combos)
    const val A: Short = 0x41; const val B: Short = 0x42; const val C: Short = 0x43
    const val D: Short = 0x44; const val E: Short = 0x45; const val F: Short = 0x46
    const val L: Short = 0x4C; const val T: Short = 0x54; const val V: Short = 0x56
    const val W: Short = 0x57; const val X: Short = 0x58; const val Z: Short = 0x5A
}
