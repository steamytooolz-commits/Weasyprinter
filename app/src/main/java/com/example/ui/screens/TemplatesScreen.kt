package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.python.CertificateFormData
import com.example.python.InvoiceFormData
import com.example.python.InvoiceItemData
import com.example.python.ReportFormData
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(viewModel: MainViewModel) {
    var selectedTemplateIndex by remember { mutableIntStateOf(0) }
    val isGenerating by viewModel.isGenerating.collectAsState()
    val lastResult by viewModel.lastGenerationResult.collectAsState()

    val invoiceState by viewModel.invoiceForm.collectAsState()
    val reportState by viewModel.reportForm.collectAsState()
    val certificateState by viewModel.certificateForm.collectAsState()

    val templateTypes = listOf("Business Invoice", "Executive Report", "Certificate")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item {
            // Header Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Article,
                        contentDescription = "Templates Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Native Python PDF Templates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Powered by Chaquopy & WeasyPrint pipeline on Android",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Template Selection Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                templateTypes.forEachIndexed { index, name ->
                    FilterChip(
                        selected = selectedTemplateIndex == index,
                        onClick = { selectedTemplateIndex = index },
                        label = { Text(name) },
                        leadingIcon = {
                            val icon = when (index) {
                                0 -> Icons.Default.Receipt
                                1 -> Icons.Default.Assessment
                                else -> Icons.Default.WorkspacePremium
                            }
                            Icon(icon, contentDescription = name, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("template_chip_$index")
                    )
                }
            }
        }

        // Template Form Editor
        item {
            when (selectedTemplateIndex) {
                0 -> InvoiceFormEditor(
                    formData = invoiceState,
                    onUpdate = { viewModel.invoiceForm.value = it }
                )
                1 -> ReportFormEditor(
                    formData = reportState,
                    onUpdate = { viewModel.reportForm.value = it }
                )
                2 -> CertificateFormEditor(
                    formData = certificateState,
                    onUpdate = { viewModel.certificateForm.value = it }
                )
            }
        }

        // Generate Button
        item {
            Button(
                onClick = {
                    when (selectedTemplateIndex) {
                        0 -> viewModel.generateInvoice()
                        1 -> viewModel.generateReport()
                        2 -> viewModel.generateCertificate()
                    }
                },
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_pdf_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Python Generating PDF...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate PDF with Python", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Generation Success Card
        item {
            AnimatedVisibility(visible = lastResult != null) {
                lastResult?.let { res ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (res.success) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (res.success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (res.success) "PDF Generated Successfully!" else "Generation Failed",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (res.success) {
                                Text(
                                    text = "Engine: ${res.engineUsed}  •  Time: ${res.durationMs}ms  •  Size: ${res.fileSizeBytes / 1024} KB",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (res.weasyprintAttempted && res.weasyprintError != null) {
                                    Text(
                                        text = "WeasyPrint notice: Native font backend bypassed; rendered using pure Python engine.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.documents.value.firstOrNull { it.filePath == res.outputPath }?.let {
                                                viewModel.openViewer(it)
                                            }
                                        },
                                        modifier = Modifier.testTag("preview_generated_pdf_button")
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View PDF")
                                    }
                                }
                            } else {
                                Text(
                                    text = res.error ?: "Unknown error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceFormEditor(
    formData: InvoiceFormData,
    onUpdate: (InvoiceFormData) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Invoice Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formData.invoiceNo,
                    onValueChange = { onUpdate(formData.copy(invoiceNo = it)) },
                    label = { Text("Invoice #") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formData.date,
                    onValueChange = { onUpdate(formData.copy(date = it)) },
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = formData.senderName,
                onValueChange = { onUpdate(formData.copy(senderName = it)) },
                label = { Text("Sender / Company") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formData.clientName,
                onValueChange = { onUpdate(formData.copy(clientName = it)) },
                label = { Text("Client Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formData.clientEmail,
                onValueChange = { onUpdate(formData.copy(clientEmail = it)) },
                label = { Text("Client Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Invoice Items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            formData.items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = item.desc,
                        onValueChange = { newDesc ->
                            val updated = formData.items.toMutableList().apply {
                                this[index] = item.copy(desc = newDesc)
                            }
                            onUpdate(formData.copy(items = updated))
                        },
                        label = { Text("Item ${index + 1}") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = item.rate.toString(),
                        onValueChange = { newRate ->
                            val rateVal = newRate.toDoubleOrNull() ?: 0.0
                            val updated = formData.items.toMutableList().apply {
                                this[index] = item.copy(rate = rateVal, amount = rateVal * item.qty)
                            }
                            onUpdate(formData.copy(items = updated))
                        },
                        label = { Text("Price ($)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = formData.notes,
                onValueChange = { onUpdate(formData.copy(notes = it)) },
                label = { Text("Payment Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}

@Composable
fun ReportFormEditor(
    formData: ReportFormData,
    onUpdate: (ReportFormData) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Executive Report Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = formData.title,
                onValueChange = { onUpdate(formData.copy(title = it)) },
                label = { Text("Report Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formData.subtitle,
                onValueChange = { onUpdate(formData.copy(subtitle = it)) },
                label = { Text("Subtitle") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formData.author,
                    onValueChange = { onUpdate(formData.copy(author = it)) },
                    label = { Text("Author / Department") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formData.date,
                    onValueChange = { onUpdate(formData.copy(date = it)) },
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Report Sections", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            formData.sections.forEachIndexed { index, sec ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = sec.heading,
                        onValueChange = { newHead ->
                            val updated = formData.sections.toMutableList().apply {
                                this[index] = sec.copy(heading = newHead)
                            }
                            onUpdate(formData.copy(sections = updated))
                        },
                        label = { Text("Section ${index + 1} Heading") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sec.body,
                        onValueChange = { newBody ->
                            val updated = formData.sections.toMutableList().apply {
                                this[index] = sec.copy(body = newBody)
                            }
                            onUpdate(formData.copy(sections = updated))
                        },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun CertificateFormEditor(
    formData: CertificateFormData,
    onUpdate: (CertificateFormData) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Certificate Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = formData.recipientName,
                onValueChange = { onUpdate(formData.copy(recipientName = it)) },
                label = { Text("Recipient Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formData.title,
                onValueChange = { onUpdate(formData.copy(title = it)) },
                label = { Text("Certificate Header") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formData.courseOrAchievement,
                onValueChange = { onUpdate(formData.copy(courseOrAchievement = it)) },
                label = { Text("Achievement / Course") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formData.issuer,
                    onValueChange = { onUpdate(formData.copy(issuer = it)) },
                    label = { Text("Issuing Organization") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = formData.signatory,
                    onValueChange = { onUpdate(formData.copy(signatory = it)) },
                    label = { Text("Signatory") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
