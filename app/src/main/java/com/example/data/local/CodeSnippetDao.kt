package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeSnippetDao {
    @Query("SELECT * FROM code_snippets ORDER BY timestamp DESC")
    fun getAllSnippets(): Flow<List<CodeSnippet>>

    @Query("SELECT * FROM code_snippets WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteSnippets(): Flow<List<CodeSnippet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: CodeSnippet): Long

    @Delete
    suspend fun deleteSnippet(snippet: CodeSnippet)

    @Query("DELETE FROM code_snippets WHERE id = :id")
    suspend fun deleteSnippetById(id: Int)

    @Update
    suspend fun updateSnippet(snippet: CodeSnippet)

    @Query("DELETE FROM code_snippets")
    suspend fun clearAllSnippets()
}
