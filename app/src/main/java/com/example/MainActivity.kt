package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.CodeSnippet
import com.example.data.local.CodeSnippetRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CodeSnippetViewModel
import com.example.ui.viewmodel.CodeSnippetViewModelFactory
import com.example.ui.viewmodel.GeneratorUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CodeSnippetRepository(database.codeSnippetDao())
        val viewModelFactory = CodeSnippetViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: CodeSnippetViewModel = viewModel(factory = viewModelFactory)
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: CodeSnippetViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val activeSavedSnippet by viewModel.activeSavedSnippet.collectAsState()

    // Automatically switch back to generator tab if user loads a saved snippet
    LaunchedEffect(activeSavedSnippet) {
        if (activeSavedSnippet != null) {
            selectedTab = 0
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            HeaderBar(activeModel = viewModel.selectedModel.collectAsState().value)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Sliding navigation tab
            CodeTabRow(
                selectedTabIndex = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> GeneratorTab(viewModel = viewModel)
                1 -> HistoryTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HeaderBar(activeModel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF06B6D4),
                                Color(0xFF2563EB)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "App logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI Code Generator",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Gemini-powered IDE Companion",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        // Active Model Status Indicator
        val modelDisplay = if (activeModel == "gemini-3.5-flash") "Flash 3.5" else "Pro 3.1"
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = modelDisplay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CodeTabRow(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Generator", "Saved Snippets")

    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                color = MaterialTheme.colorScheme.primary,
                height = 3.dp
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTabIndex == index
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("tab_$index"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (index == 0) Icons.Default.Code else Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = textColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GeneratorTab(viewModel: CodeSnippetViewModel) {
    val prompt by viewModel.prompt.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activeSavedSnippet by viewModel.activeSavedSnippet.collectAsState()

    val context = LocalContext.current

    val languages = listOf("Kotlin", "Python", "JavaScript", "HTML/CSS", "Java", "C++", "Go", "Rust", "SQL", "Swift")
    val styles = listOf("Optimized", "Clean & Simple", "Detailed & Comments", "Beginner Friendly")
    val models = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview")

    val quickPrompts = listOf(
        "Binary search function",
        "Debounce execution wrapper",
        "Fetch JSON REST API client",
        "SQLite database helper table",
        "Regular expression for Email validation"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Saved Snippet Banner Indicator
        if (activeSavedSnippet != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Viewing Saved Snippet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearActiveSavedSnippet() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Prompt Input Container
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Coding Request",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { viewModel.onPromptChange(it) },
                        placeholder = {
                            Text(
                                "Describe what code you want to generate...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp)
                            .testTag("prompt_input"),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    )

                    // Quick prompt chips
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Suggestions",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPrompts.forEach { qp ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable { viewModel.onPromptChange(qp) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = qp,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Language Row Selection
        item {
            Column {
                Text(
                    text = "Target Language",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(languages) { lang ->
                        val isSelected = selectedLanguage == lang
                        val chipColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        val textColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipColor)
                                .clickable { viewModel.onLanguageChange(lang) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("lang_chip_$lang"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // Options Row (Model Selector & Style Selector)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Style selection dropdown
                var styleExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = styleExpanded,
                        onExpandedChange = { styleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedStyle,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Output Style", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = styleExpanded,
                            onDismissRequest = { styleExpanded = false }
                        ) {
                            styles.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(style, fontSize = 13.sp) },
                                    onClick = {
                                        viewModel.onStyleChange(style)
                                        styleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Model selection dropdown
                var modelExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (selectedModel == "gemini-3.5-flash") "Gemini Flash 3.5" else "Gemini Pro 3.1",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("AI Model", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (model == "gemini-3.5-flash") "Gemini Flash (Fast)" else "Gemini Pro (Smart)",
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        viewModel.onModelChange(model)
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Button: Generate Code
        item {
            val isLoading = uiState is GeneratorUiState.Loading

            Button(
                onClick = { viewModel.generateCode() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLoading) "Synthesizing Code..." else "Generate Code",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // UI Outputs
        item {
            when (val state = uiState) {
                is GeneratorUiState.Idle -> {
                    EmptyStateCard()
                }
                is GeneratorUiState.Loading -> {
                    LoadingStateCard()
                }
                is GeneratorUiState.Success -> {
                    CodeOutputCard(
                        prompt = prompt,
                        language = selectedLanguage,
                        code = state.code,
                        explanation = state.explanation,
                        activeSavedSnippet = activeSavedSnippet,
                        onToggleFavorite = {
                            activeSavedSnippet?.let {
                                viewModel.toggleFavorite(it)
                            } ?: run {
                                Toast.makeText(context, "Snippet auto-saved to Saved Snippets! You can star it there.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                is GeneratorUiState.Error -> {
                    ErrorStateCard(message = state.message)
                }
            }
        }

        // Padding at the bottom
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.code_hero_banner),
                contentDescription = "Code Companion Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "AI Coding Engine Ready",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Describe your desired script or logical routine, choose a programming language, and tap 'Generate Code' to construct full, safe, optimized algorithms in real-time.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.alpha(0.8f)
                )
            }
        }
    }
}

@Composable
fun LoadingStateCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(pulseAlpha)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Generating logic structures...",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(pulseAlpha)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Parsing requests and compiling optimal algorithms with Gemini models...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.7f)
            )
        }
    }
}

@Composable
fun ErrorStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Generation Interrupted",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun CodeOutputCard(
    prompt: String,
    language: String,
    code: String,
    explanation: String,
    activeSavedSnippet: CodeSnippet?,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Code Block Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C)), // Standard IDE deep space slate
            shape = RoundedCornerShape(16.dp),
            border = borderStroke()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Code block header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF162030))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF06B6D4))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = language,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Header buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Favorite button
                        val isFav = activeSavedSnippet?.isFavorite ?: false
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color(0xFFEF4444) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Copy button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Generated Code", code)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Share button
                        IconButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "--- Generated $language Code ---\n\n$code")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Code text content with custom line number display and syntax highlighting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Line numbers
                    val lines = code.split("\n")
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.alpha(0.35f)
                    ) {
                        for (i in 1..lines.size) {
                            Text(
                                text = "$i",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Code output with keyword highlighting
                    HighlightedCodeText(
                        code = code,
                        language = language,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Explanation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Explanation & Complexity Analysis",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                MarkdownText(text = explanation)
            }
        }
    }
}

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
)

@Composable
fun HighlightedCodeText(code: String, language: String, modifier: Modifier = Modifier) {
    val annotatedString = remember(code, language) {
        buildAnnotatedString {
            val keywords = when (language.lowercase()) {
                "kotlin", "java", "swift" -> listOf("package", "import", "class", "interface", "fun", "val", "var", "return", "if", "else", "for", "while", "when", "is", "null", "true", "false", "private", "public", "protected", "override", "suspend", "object", "companion", "let")
                "python" -> listOf("import", "from", "def", "class", "return", "if", "elif", "else", "for", "while", "in", "is", "not", "and", "or", "try", "except", "lambda", "None", "True", "False")
                "javascript", "typescript", "html/css" -> listOf("import", "export", "const", "let", "var", "function", "return", "if", "else", "for", "while", "class", "extends", "true", "false", "null", "undefined", "async", "await")
                else -> listOf("import", "class", "return", "if", "else", "for", "while", "true", "false")
            }

            var index = 0
            val pattern = "(\\w+)|(\\W+)".toRegex()
            pattern.findAll(code).forEach { match ->
                val word = match.value
                when {
                    word in keywords -> {
                        pushStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold))
                        append(word)
                        pop()
                    }
                    word.startsWith("//") || word.startsWith("#") || (word.startsWith("/*") && word.endsWith("*/")) -> {
                        pushStyle(SpanStyle(color = Color(0xFF34D399)))
                        append(word)
                        pop()
                    }
                    word.startsWith("\"") && word.endsWith("\"") -> {
                        pushStyle(SpanStyle(color = Color(0xFFFBBF24)))
                        append(word)
                        pop()
                    }
                    word.toIntOrNull() != null -> {
                        pushStyle(SpanStyle(color = Color(0xFFF472B6)))
                        append(word)
                        pop()
                    }
                    else -> {
                        append(word)
                    }
                }
            }
        }
    }

    SelectionContainer {
        Text(
            text = annotatedString,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFFE2E8F0),
            modifier = modifier,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(modifier = modifier) {
        lines.forEach { line ->
            when {
                line.startsWith("###") -> {
                    Text(
                        text = line.removePrefix("###").trim(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                line.startsWith("##") -> {
                    Text(
                        text = line.removePrefix("##").trim(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                line.startsWith("#") -> {
                    Text(
                        text = line.removePrefix("#").trim(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val content = line.substring(2)
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                        Text(text = "• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = parseBoldText(content),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
                else -> {
                    if (line.trim().isNotEmpty()) {
                        Text(
                            text = parseBoldText(line),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 3.dp),
                            lineHeight = 18.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun parseBoldText(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        var isBold = false
        parts.forEach { part ->
            if (isBold) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                append(part)
                pop()
            } else {
                append(part)
            }
            isBold = !isBold
        }
    }
}

@Composable
fun HistoryTab(viewModel: CodeSnippetViewModel) {
    val history by viewModel.history.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }

    val activeList = if (favoritesOnly) favorites else history
    val filteredList = activeList.filter {
        it.prompt.contains(searchQuery, ignoreCase = true) ||
                it.language.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter options
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search saved code...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(12.dp),
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("search_history"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            )

            // Star / Filter favorites button
            IconButton(
                onClick = { favoritesOnly = !favoritesOnly },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (favoritesOnly) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (favoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .testTag("filter_favorites")
            ) {
                Icon(
                    imageVector = if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Show Favorites Only",
                    tint = if (favoritesOnly) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History content list
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matches found" else if (favoritesOnly) "No bookmarked snippets yet" else "History is empty",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Your generated programs will be saved here offline.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (favoritesOnly) "Bookmarked Scripts (${filteredList.size})" else "Recent Generations (${filteredList.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!favoritesOnly) {
                    Text(
                        text = "Clear All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { viewModel.clearHistory() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { snippet ->
                    HistoryCard(
                        snippet = snippet,
                        onSelect = { viewModel.selectSnippet(snippet) },
                        onToggleFavorite = { viewModel.toggleFavorite(snippet) },
                        onDelete = { viewModel.deleteSnippet(snippet) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    snippet: CodeSnippet,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("history_card_${snippet.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = borderStroke()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = snippet.language,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (snippet.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (snippet.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = snippet.prompt,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = snippet.code,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp)
            )
        }
    }
}
