package com.example.data.local

import kotlinx.coroutines.flow.Flow

class CodeSnippetRepository(private val codeSnippetDao: CodeSnippetDao) {
    val allSnippets: Flow<List<CodeSnippet>> = codeSnippetDao.getAllSnippets()
    val favoriteSnippets: Flow<List<CodeSnippet>> = codeSnippetDao.getFavoriteSnippets()

    suspend fun insert(snippet: CodeSnippet): Long {
        return codeSnippetDao.insertSnippet(snippet)
    }

    suspend fun delete(snippet: CodeSnippet) {
        codeSnippetDao.deleteSnippet(snippet)
    }

    suspend fun deleteById(id: Int) {
        codeSnippetDao.deleteSnippetById(id)
    }

    suspend fun update(snippet: CodeSnippet) {
        codeSnippetDao.updateSnippet(snippet)
    }

    suspend fun clearAll() {
        codeSnippetDao.clearAllSnippets()
    }
}
