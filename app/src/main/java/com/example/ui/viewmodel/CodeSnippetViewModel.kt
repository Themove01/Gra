package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.local.CodeSnippet
import com.example.data.local.CodeSnippetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GeneratorUiState {
    object Idle : GeneratorUiState
    object Loading : GeneratorUiState
    data class Success(val code: String, val explanation: String) : GeneratorUiState
    data class Error(val message: String) : GeneratorUiState
}

class CodeSnippetViewModel(private val repository: CodeSnippetRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Idle)
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    // Configuration states
    val prompt = MutableStateFlow("")
    val selectedLanguage = MutableStateFlow("Kotlin")
    val selectedModel = MutableStateFlow("gemini-3.5-flash")
    val selectedStyle = MutableStateFlow("Optimized")

    // Saved snippet being actively displayed, if any
    private val _activeSavedSnippet = MutableStateFlow<CodeSnippet?>(null)
    val activeSavedSnippet: StateFlow<CodeSnippet?> = _activeSavedSnippet.asStateFlow()

    // History flows from Room database
    val history: StateFlow<List<CodeSnippet>> = repository.allSnippets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favorites: StateFlow<List<CodeSnippet>> = repository.favoriteSnippets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onPromptChange(newPrompt: String) {
        prompt.value = newPrompt
    }

    fun onLanguageChange(lang: String) {
        selectedLanguage.value = lang
    }

    fun onModelChange(model: String) {
        selectedModel.value = model
    }

    fun onStyleChange(style: String) {
        selectedStyle.value = style
    }

    fun selectSnippet(snippet: CodeSnippet) {
        _activeSavedSnippet.value = snippet
        prompt.value = snippet.prompt
        selectedLanguage.value = snippet.language
        _uiState.value = GeneratorUiState.Success(snippet.code, snippet.explanation)
    }

    fun clearActiveSavedSnippet() {
        _activeSavedSnippet.value = null
        _uiState.value = GeneratorUiState.Idle
    }

    fun generateCode() {
        val currentPrompt = prompt.value.trim()
        if (currentPrompt.isEmpty()) {
            _uiState.value = GeneratorUiState.Error("Please enter a coding request first.")
            return
        }

        val lang = selectedLanguage.value
        val model = selectedModel.value
        val style = selectedStyle.value

        _uiState.value = GeneratorUiState.Loading
        _activeSavedSnippet.value = null // Generating a new one clears selected saved snippet

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _uiState.value = GeneratorUiState.Error("Gemini API key is not configured. Please add it via the Secrets panel.")
                    return@launch
                }

                val fullPrompt = """
                    Write a code snippet based on the following request:
                    Request: $currentPrompt
                    Programming Language: $lang
                    Coding Style / Optimization focus: $style
                    
                    Instructions for output format:
                    Please follow the response tags strictly so I can parse your response.
                    Wrap the code inside [CODE_START] and [CODE_END] tags. Do NOT wrap code block inside markdown backticks inside these tags, just plain raw code.
                    Wrap the explanation, complexity analysis, and description inside [EXPLANATION_START] and [EXPLANATION_END] tags.
                    
                    Example output format:
                    [CODE_START]
                    fun greet() { println("Hello") }
                    [CODE_END]
                    [EXPLANATION_START]
                    This is a simple function to greet the user. It has O(1) complexity.
                    [EXPLANATION_END]
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = fullPrompt))))
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(model, apiKey, request)
                }

                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText != null) {
                    val (code, explanation) = parseResponse(rawText)
                    _uiState.value = GeneratorUiState.Success(code, explanation)

                    // Auto-save generated snippet to history
                    withContext(Dispatchers.IO) {
                        repository.insert(
                            CodeSnippet(
                                prompt = currentPrompt,
                                language = lang,
                                code = code,
                                explanation = explanation
                            )
                        )
                    }
                } else {
                    _uiState.value = GeneratorUiState.Error("Empty response from Gemini. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = GeneratorUiState.Error(e.message ?: "An unexpected error occurred during code generation.")
            }
        }
    }

    fun toggleFavorite(snippet: CodeSnippet) {
        viewModelScope.launch {
            val updated = snippet.copy(isFavorite = !snippet.isFavorite)
            repository.update(updated)
            // If the toggled snippet is the currently viewed snippet, update its active status
            if (_activeSavedSnippet.value?.id == snippet.id) {
                _activeSavedSnippet.value = updated
            }
        }
    }

    fun deleteSnippet(snippet: CodeSnippet) {
        viewModelScope.launch {
            repository.delete(snippet)
            if (_activeSavedSnippet.value?.id == snippet.id) {
                _activeSavedSnippet.value = null
                _uiState.value = GeneratorUiState.Idle
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _activeSavedSnippet.value = null
            _uiState.value = GeneratorUiState.Idle
        }
    }

    private fun parseResponse(responseText: String): Pair<String, String> {
        val codeStart = "[CODE_START]"
        val codeEnd = "[CODE_END]"
        val explanationStart = "[EXPLANATION_START]"
        val explanationEnd = "[EXPLANATION_END]"

        val codeIndex = responseText.indexOf(codeStart)
        val codeEndIndex = responseText.indexOf(codeEnd)
        val expIndex = responseText.indexOf(explanationStart)
        val expEndIndex = responseText.indexOf(explanationEnd)

        var code = ""
        var explanation = ""

        if (codeIndex != -1 && codeEndIndex != -1) {
            code = responseText.substring(codeIndex + codeStart.length, codeEndIndex).trim()
        }
        if (expIndex != -1 && expEndIndex != -1) {
            explanation = responseText.substring(expIndex + explanationStart.length, expEndIndex).trim()
        }

        // If formatting fails, fallback to markdown parsing
        if (code.isEmpty() && explanation.isEmpty()) {
            val markdownCodeRegex = "```[a-zA-Z]*\\n([\\s\\S]*?)\\n```".toRegex()
            val matchResult = markdownCodeRegex.find(responseText)
            if (matchResult != null) {
                code = matchResult.groupValues[1].trim()
                explanation = responseText.replace(matchResult.value, "").trim()
            } else {
                code = responseText
                explanation = "Generative model output."
            }
        }

        return Pair(code, explanation)
    }
}

class CodeSnippetViewModelFactory(private val repository: CodeSnippetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodeSnippetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodeSnippetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
