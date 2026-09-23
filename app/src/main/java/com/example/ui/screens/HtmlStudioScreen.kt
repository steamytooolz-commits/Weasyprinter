package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlStudioScreen(viewModel: MainViewModel) {
    var editorSubTab by remember { mutableIntStateOf(0) } // 0: HTML, 1: CSS, 2: Live Preview
    val htmlText by viewModel.htmlCode.collectAsState()
    val cssText by viewModel.cssCode.collectAsState()
    val docTitle by viewModel.customDocTitle.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val lastResult by viewModel.lastGenerationResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Presets horizontal selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { viewModel.loadSampleTemplate("invoice") },
                label = { Text("Invoice") },
                icon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            SuggestionChip(
                onClick = { viewModel.loadSampleTemplate("badge") },
                label = { Text("VIP Badge") },
                icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            SuggestionChip(
                onClick = { viewModel.loadSampleTemplate("receipt") },
                label = { Text("POS Receipt") },
                icon = { Icon(Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            SuggestionChip(
                onClick = { viewModel.loadSampleTemplate("letter") },
                label = { Text("Letter") },
                icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Title and Engine bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = docTitle,
                onValueChange = { viewModel.customDocTitle.value = it },
                label = { Text("Document Title") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { viewModel.generateCustomHtml() },
                enabled = !isGenerating,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("compile_html_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compile PDF", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Editor / Preview Tabs
        TabRow(
            selectedTabIndex = editorSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = editorSubTab == 0,
                onClick = { editorSubTab = 0 },
                text = { Text("HTML Source") },
                icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = editorSubTab == 1,
                onClick = { editorSubTab = 1 },
                text = { Text("CSS Styles") },
                icon = { Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = editorSubTab == 2,
                onClick = { editorSubTab = 2 },
                text = { Text("Live Preview") },
                icon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (editorSubTab) {
                0 -> {
                    OutlinedTextField(
                        value = htmlText,
                        onValueChange = { viewModel.htmlCode.value = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("html_source_editor"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        placeholder = { Text("Enter HTML markup here...") }
                    )
                }
                1 -> {
                    OutlinedTextField(
                        value = cssText,
                        onValueChange = { viewModel.cssCode.value = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("css_source_editor"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        placeholder = { Text("Enter CSS styles (@page, typography, colors)...") }
                    )
                }
                2 -> {
                    // WebView live preview
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = false
                                }
                            },
                            update = { webView ->
                                val fullHtml = "<!DOCTYPE html><html><head><meta charset='utf-8'><style>$cssText</style></head><body>$htmlText</body></html>"
                                webView.loadDataWithBaseURL(null, fullHtml, "text/html", "utf-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Status pill
        lastResult?.let { res ->
            if (res.docType == "CUSTOM_HTML") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (res.success) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (res.success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (res.success) "PDF Compiled via ${res.engineUsed} (${res.durationMs}ms)" else "Error: ${res.error}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
