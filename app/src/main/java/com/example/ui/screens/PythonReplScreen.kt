package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel

@Composable
fun PythonReplScreen(viewModel: MainViewModel) {
    val scriptCode by viewModel.pythonScript.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val execResult by viewModel.lastExecutionResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Preset examples bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { viewModel.loadReplExample("fpdf2") },
                label = { Text("FPDF2 Canvas") },
                icon = { Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            SuggestionChip(
                onClick = { viewModel.loadReplExample("reportlab") },
                label = { Text("ReportLab Tables") },
                icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            SuggestionChip(
                onClick = { viewModel.loadReplExample("weasyprint") },
                label = { Text("WeasyPrint Test") },
                icon = { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            SuggestionChip(
                onClick = { viewModel.loadReplExample("jinja") },
                label = { Text("Jinja2 Rendering") },
                icon = { Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Python Script Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.runPythonScript() },
                enabled = !isGenerating,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("run_python_script_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Executing...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Python", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Code Editor
        OutlinedTextField(
            value = scriptCode,
            onValueChange = { viewModel.pythonScript.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("python_code_editor"),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            placeholder = { Text("# Write or paste Python code here...") }
        )

        // Terminal Output Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 200.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONSOLE OUTPUT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    execResult?.let { res ->
                        Text(
                            text = "${res.durationMs}ms • ${if (res.success) "SUCCESS" else "FAILED"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (res.success) Color(0xFF34D399) else Color(0xFFF87171)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val outText = buildString {
                        execResult?.let { res ->
                            if (res.stdout.isNotBlank()) append(res.stdout).append("\n")
                            if (res.stderr.isNotBlank()) append(res.stderr).append("\n")
                            if (res.pdfCreated) append(">> PDF file generated successfully: ${res.fileSizeBytes / 1024} KB")
                        } ?: append("Ready. Press 'Run Python' to execute script via Chaquopy.")
                    }
                    Text(
                        text = outText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFF1F5F9)
                    )
                }

                // If PDF was created, provide quick view button
                execResult?.let { res ->
                    if (res.pdfCreated && res.outputPdfPath != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FilledTonalButton(
                            onClick = {
                                viewModel.documents.value.firstOrNull { it.filePath == res.outputPdfPath }?.let {
                                    viewModel.openViewer(it)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("view_script_output_pdf")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Generated Script PDF")
                        }
                    }
                }
            }
        }
    }
}
