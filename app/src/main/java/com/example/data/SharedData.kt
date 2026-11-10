package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity to store incoming WhatsApp shares (text, links, images, videos, documents).
 */
@Entity(tableName = "shared_data")
data class SharedData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val contentType: String, // "TEXT", "LINK", "IMAGE", "VIDEO", "AUDIO", "DOCUMENT"
    val rawContent: String,
    val sourcePackage: String = "com.whatsapp",
    val titleOrSubject: String = ""
)
