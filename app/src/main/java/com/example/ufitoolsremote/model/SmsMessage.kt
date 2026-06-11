package com.example.ufitoolsremote.model

import kotlinx.serialization.Serializable

@Serializable
data class SmsMessage(
    val id: String,
    val number: String,
    val content: String,
    val rawContent: String,
    val date: String,
    val tag: String,
    val isUnread: Boolean,
    val isFailed: Boolean
)
