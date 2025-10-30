package com.example.myapplication
import kotlinx.serialization.Serializable


@Serializable data class JournalRequest(val text: String)
@Serializable data class JournalResponse(val emotion: String, val advice: String)
