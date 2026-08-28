package com.wallpaperextend.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wallpaperextend.ai.AIModel
import com.wallpaperextend.ai.AIApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.asImageBitmap

sealed class AIScreen {
    object Settings : AIScreen()
    object Processing : AIScreen()
    object Result : AIScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreenWrapper(
    onBack: () -> Unit,
    originalBitmap: android.graphics.Bitmap?
) {
    val context = LocalContext.current
    val aiViewModel: AIViewModel = viewModel(factory = AIViewModelFactory(context))

    var currentScreen by remember { mutableStateOf<AIScreen>(AIScreen.Settings) }
    var showModelDialog by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(aiViewModel.defaultModels.first()) }
    var apiEndpoint by remember { mutableStateOf("https://api.openai.com/v1/images/variations") }
    var apiKey by remember { mutableStateOf("") }
    var showApiKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentScreen) {
        if (currentScreen == AIScreen.Processing && originalBitmap != null) {
            aiViewModel.processWithAI(
                context = context,
                config = AIApiConfig(
                    endpoint = apiEndpoint,
                    apiKey = apiKey,
                    model = selectedModel.id
                ),
                imageBytes = withContext(Dispatchers.IO) {
                    val bytes = java.io.ByteArrayOutputStream().also { os ->
                        originalBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os)
                    }.toByteArray()
                    bytes
                }
            )
            if (aiViewModel.resultImageUri != null) {
                currentScreen = AIScreen.Result
            } else {
                currentScreen = AIScreen.Settings
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 图像扩展", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        when (currentScreen) {
            is AIScreen.Settings -> AISettingsScreen(
                apiEndpoint = apiEndpoint,
                onEndpointChange = { apiEndpoint = it },
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                showApiKeyVisible = showApiKeyVisible,
                onToggleApiKeyVisibility = { showApiKeyVisible = !showApiKeyVisible },
                selectedModel = selectedModel,
                onModelSelected = { selectedModel = it },
                onProcess = { currentScreen = AIScreen.Processing },
                isProcessing = false,
                defaultModels = aiViewModel.defaultModels
            )
            is AIScreen.Processing -> AIScreenProcessing(
                errorMessage = aiViewModel.errorMessage,
                onBack = { currentScreen = AIScreen.Settings }
            )
            is AIScreen.Result -> AIScreenResult(
                resultUri = aiViewModel.resultImageUri ?: "",
                onBack = { currentScreen = AIScreen.Settings },
                onSave = { /* TODO: implement save */ }
            )
        }
    }

    if (showModelDialog) {
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            title = { Text("选择模型", color = Color.White) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(aiViewModel.defaultModels) { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedModel.id == model.id) Color(0xFF0A84FF) else Color(0xFF2C2C2E))
                                .clickable {
                                    selectedModel = model
                                    showModelDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = model.provider,
                                    color = Color(0xFF8E8E93),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showModelDialog = false }) {
                    Text("确定", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun AISettingsScreen(
    apiEndpoint: String,
    onEndpointChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showApiKeyVisible: Boolean,
    onToggleApiKeyVisibility: () -> Unit,
    selectedModel: AIModel,
    onModelSelected: (AIModel) -> Unit,
    onProcess: () -> Unit,
    isProcessing: Boolean,
    defaultModels: List<AIModel>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = apiEndpoint,
            onValueChange = onEndpointChange,
            label = { Text("API 端点", color = Color(0xFF8E8E93)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedLabelColor = Color(0xFF0A84FF),
                unfocusedLabelColor = Color(0xFF8E8E93),
                focusedBorderColor = Color(0xFF0A84FF),
                unfocusedBorderColor = Color(0xFF3A3A3C)
            )
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API 密钥", color = Color(0xFF8E8E93)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showApiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = onToggleApiKeyVisibility) {
                    Icon(
                        imageVector = if (showApiKeyVisible) androidx.compose.material.icons.Icons.Default.Visibility else androidx.compose.material.icons.Icons.Default.VisibilityOff,
                        contentDescription = "显示/隐藏",
                        tint = Color(0xFF8E8E93)
                    )
                }
            },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedLabelColor = Color(0xFF0A84FF),
                unfocusedLabelColor = Color(0xFF8E8E93),
                focusedBorderColor = Color(0xFF0A84FF),
                unfocusedBorderColor = Color(0xFF3A3A3C)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "模型: ${selectedModel.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Button(
                onClick = { onModelSelected(selectedModel) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Text("选择模型", color = Color(0xFF0A84FF))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onProcess,
            enabled = apiKey.isNotEmpty() && !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("开始处理", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun AIScreenProcessing(errorMessage: String?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF0A84FF), strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "AI 处理中...",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Text("返回", color = Color.White)
            }
        }
    }
}

@Composable
fun AIScreenResult(resultUri: String, onBack: () -> Unit, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (resultUri.isNotEmpty()) {
                androidx.compose.foundation.Image(
                    bitmap = android.graphics.BitmapFactory.decodeFile(resultUri)?.asImageBitmap()
                        ?: androidx.compose.ui.graphics.ImageBitmap(1, 1),
                    contentDescription = "AI 结果",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))
        ) {
            Text("保存到相册", fontSize = 16.sp, color = Color.White)
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Text("返回设置", fontSize = 16.sp, color = Color.White)
        }
    }
}

class AIViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return AIViewModel() as T
    }
}