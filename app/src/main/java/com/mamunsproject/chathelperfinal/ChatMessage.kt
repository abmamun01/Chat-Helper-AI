package com.mamunsproject.chathelperfinal

import android.graphics.Rect

data class ChatMessage(
    val text: String,
    val sender: String,
    val timestamp: String = "",
    val isRightAligned: Boolean,
    val bounds: Rect = Rect()
)
