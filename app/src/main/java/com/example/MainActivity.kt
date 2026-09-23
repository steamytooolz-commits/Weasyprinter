package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FullScreenPdfDialog
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val selectedTab by viewModel.selectedTab.collectAsState()
                val viewingDoc by viewModel.viewingDoc.collectAsState()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = "PyPrint Logo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "PyPrint",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Chaquopy • WeasyPrint • ReportLab",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.setTab(4) },
                                    modifier = Modifier.testTag("nav_diagnostics_icon")
                                ) {
                                    Icon(
                                        Icons.Default.Memory,
                                        contentDescription = "Engine Diagnostics",
                                        tint = if (selectedTab == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { viewModel.setTab(0) },
                                icon = { Icon(Icons.Default.Article, contentDescription = "Templates") },
                                label = { Text("Templates", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_templates")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { viewModel.setTab(1) },
                                icon = { Icon(Icons.Default.Code, contentDescription = "HTML & CSS Studio") },
                                label = { Text("HTML/CSS", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_html_studio")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { viewModel.setTab(2) },
                                icon = { Icon(Icons.Default.Terminal, contentDescription = "Python REPL") },
                                label = { Text("Python REPL", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_python_repl")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { viewModel.setTab(3) },
                                icon = { Icon(Icons.Default.FolderShared, contentDescription = "Document Library") },
                                label = { Text("PDF Library", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_library")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { viewModel.setTab(4) },
                                icon = { Icon(Icons.Default.SettingsSuggest, contentDescription = "Diagnostics") },
                                label = { Text("Runtime", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_diagnostics")
                            )
                        }
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (selectedTab) {
                            0 -> TemplatesScreen(viewModel)
                            1 -> HtmlStudioScreen(viewModel)
                            2 -> PythonReplScreen(viewModel)
                            3 -> DocumentLibraryScreen(viewModel)
                            4 -> DiagnosticsScreen(viewModel)
                        }

                        // Full Screen Native PDF Viewer Dialog
                        viewingDoc?.let { doc ->
                            FullScreenPdfDialog(
                                filePath = doc.filePath,
                                title = doc.title,
                                engineUsed = doc.engineUsed,
                                onDismiss = { viewModel.closeViewer() }
                            )
                        }
                    }
                }
            }
        }
    }
}
