package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val date: String,
    val morningNote: String? = null,
    val eveningNote: String? = null,
    val moodScore: Int? = null,
    val tagsJson: String = "[]",
    val customTagsJson: String = "[]"
)
