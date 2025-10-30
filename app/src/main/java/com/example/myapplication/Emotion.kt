package com.example.myapplication

import androidx.compose.ui.graphics.Color

enum class Emotion {  JOY, SADNESS, ANGER, FEAR, DISGUST, SURPRISE, NEUTRAL }

    data class JournalEntry(
        val id: Long,
        val text: String,
        val emotion: Emotion,
        val advice: String,
        val timestamp: Long
    )

val emotionColours = mapOf(
    Emotion.JOY to Color(0xFF81C784),
    Emotion.ANGER to Color(0xFFE57373),
    Emotion.SADNESS to Color(0xFF64B5F6),
    Emotion.FEAR to Color(0xFFFFD54F),
    Emotion.DISGUST to Color(0xFF4DB6AC),
    Emotion.SURPRISE to Color(0xFFBA68C8),
    Emotion.NEUTRAL to Color(0xFFB0BEC5)
)

val emotionEmoji = mapOf(
    Emotion.JOY to "😊",
    Emotion.SADNESS to "😔",
    Emotion.ANGER to "😡",
    Emotion.FEAR to "😨",
    Emotion.DISGUST to "🤢",
    Emotion.SURPRISE to "😲",
    Emotion.NEUTRAL to "😐"
)