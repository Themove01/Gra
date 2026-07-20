package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "code_snippets")
data class CodeSnippet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val language: String,
    val code: String,
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
