package com.smoketracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smoketracker.ui.theme.SmokeTrackerTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private lateinit var smokeRepository: SmokeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        smokeRepository = SmokeRepository(this)
        
        setContent {
            SmokeTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VapeTrackerApp(smokeRepository)
                }
            }
        }
    }
}

enum class TabItem(val title: String, val icon: ImageVector) {
    Main("Main", Icons.Default.Home),
    History("History", Icons.Default.History),
    Statistics("Stats", Icons.Default.BarChart),
    Export("Export", Icons.Default.SwapHoriz)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VapeTrackerApp(repository: SmokeRepository) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = TabItem.entries
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vape Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> MainScreen(repository)
                1 -> HistoryScreen(repository)
                2 -> StatisticsScreen(repository)
                3 -> ExportImportScreen(repository)
            }
        }
    }
}

@Composable
fun MainScreen(repository: SmokeRepository) {
    var smokesToday by remember { mutableIntStateOf(repository.getSmokesToday()) }
    var lastSmokeTime by remember { mutableStateOf(repository.getLastSmokeTime()) }
    var timeSinceLastSmoke by remember { mutableStateOf("--:--") }
    
    LaunchedEffect(lastSmokeTime) {
        while (true) {
            timeSinceLastSmoke = calculateTimeSinceLastSmoke(lastSmokeTime)
            delay(1000)
        }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            smokesToday = repository.getSmokesToday()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                repository.logSmoke()
                smokesToday = repository.getSmokesToday()
                lastSmokeTime = repository.getLastSmokeTime()
            },
            modifier = Modifier.size(200.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 2.dp
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Vape",
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VAPE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Minus button under vape button
        FilledTonalIconButton(
            onClick = {
                val newLastTime = repository.decrementSmoke()
                smokesToday = repository.getSmokesToday()
                lastSmokeTime = newLastTime
            },
            enabled = smokesToday > 0,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Reduce count"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Time since last vape",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeSinceLastSmoke,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Vapes today",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = smokesToday.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(repository: SmokeRepository) {
    var todayTimestamps by remember { mutableStateOf(repository.getTodayTimestamps()) }
    val history = remember { repository.getHistory() }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val today = LocalDate.now()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Vape History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val todayCount = todayTimestamps.size
        val hasHistory = history.isNotEmpty() || todayCount > 0
        
        if (!hasHistory) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history yet",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Today's individual timestamps
                if (todayTimestamps.isNotEmpty()) {
                    item {
                        Text(
                            text = "Today",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(todayTimestamps.sortedDescending()) { timestamp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Vape",
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = timestamp.format(timeFormatter),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                
                // Previous days
                val previousDays = history.filter { it.first != today }
                if (previousDays.isNotEmpty()) {
                    item {
                        Text(
                            text = "Previous Days",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(previousDays.sortedByDescending { it.first }) { (date, count) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = date.format(dateFormatter),
                                    fontSize = 16.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = count.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class DateRange(val label: String, val days: Int) {
    WEEK("7 Days", 7),
    TWO_WEEKS("14 Days", 14),
    MONTH("30 Days", 30),
    THREE_MONTHS("90 Days", 90)
}

@Composable
fun StatisticsScreen(repository: SmokeRepository) {
    var selectedRange by remember { mutableStateOf(DateRange.WEEK) }
    val history = remember { repository.getHistory() }
    
    val rangeData = remember(selectedRange) {
        val today = LocalDate.now()
        (0 until selectedRange.days).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val count = history.find { it.first == date }?.second ?: 0
            date to count
        }.reversed()
    }
    
    val maxCount = (rangeData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Statistics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Date range selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateRange.entries.forEach { range ->
                FilterChip(
                    onClick = { selectedRange = range },
                    label = { Text(range.label, fontSize = 12.sp) },
                    selected = selectedRange == range,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val totalInRange = rangeData.sumOf { it.second }
            val avgPerDay = if (rangeData.isNotEmpty()) totalInRange.toFloat() / rangeData.size else 0f
            
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total", fontSize = 14.sp)
                    Text(
                        text = totalInRange.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Avg/day", fontSize = 14.sp)
                    Text(
                        text = String.format("%.1f", avgPerDay),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Line chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daily Vapes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val width = size.width
                    val height = size.height
                    val padding = 40f
                    val chartWidth = width - padding * 2
                    val chartHeight = height - padding * 2
                    
                    // Draw grid lines
                    for (i in 0..4) {
                        val y = padding + chartHeight * (1 - i / 4f)
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.1f),
                            start = Offset(padding, y),
                            end = Offset(width - padding, y),
                            strokeWidth = 1f
                        )
                    }
                    
                    // Draw line chart
                    if (rangeData.isNotEmpty() && rangeData.size > 1) {
                        val points = rangeData.mapIndexed { index, (_, count) ->
                            val x = padding + (chartWidth * index / (rangeData.size - 1).coerceAtLeast(1))
                            val y = padding + chartHeight * (1 - count.toFloat() / maxCount)
                            Offset(x, y)
                        }
                        
                        // Draw line
                        val path = Path()
                        points.forEachIndexed { index, point ->
                            if (index == 0) {
                                path.moveTo(point.x, point.y)
                            } else {
                                path.lineTo(point.x, point.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3f)
                        )
                        
                        // Draw points (only for smaller ranges)
                        if (rangeData.size <= 14) {
                            points.forEach { point ->
                                drawCircle(
                                    color = primaryColor,
                                    radius = 6f,
                                    center = point
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3f,
                                    center = point
                                )
                            }
                        }
                    }
                    
                    // Draw labels for start and end dates
                    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
                    if (rangeData.isNotEmpty()) {
                        drawContext.canvas.nativeCanvas.apply {
                            // Start date
                            drawText(
                                rangeData.first().first.format(dateFormatter),
                                padding,
                                height - 5f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.LEFT
                                }
                            )
                            // End date
                            drawText(
                                rangeData.last().first.format(dateFormatter),
                                width - padding,
                                height - 5f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Min/Max stats
        val maxDay = rangeData.maxByOrNull { it.second }
        val minDay = rangeData.filter { it.second > 0 }.minByOrNull { it.second }
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Most", fontSize = 12.sp)
                    Text(
                        text = maxDay?.second?.toString() ?: "-",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = maxDay?.first?.format(dateFormatter) ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Least", fontSize = 12.sp)
                    Text(
                        text = minDay?.second?.toString() ?: "-",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = minDay?.first?.format(dateFormatter) ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExportImportScreen(repository: SmokeRepository) {
    val context = LocalContext.current
    var exportData by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    
    // File picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(exportData.toByteArray())
                }
                Toast.makeText(context, "Data exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().readText()
                    if (repository.importData(jsonString)) {
                        Toast.makeText(context, "Data imported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Import failed: Invalid data format", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Export / Import",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Export section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Export Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save your vape tracking data to a file for backup",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        exportData = repository.exportData()
                        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        exportLauncher.launch("vape_tracker_backup_$dateStr.json")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export to File")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        exportData = repository.exportData()
                        showExportDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show Export Data")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Import section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Import Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Restore your vape tracking data from a backup file",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import from File")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        importText = ""
                        showImportDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Paste Import Data")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Backup Info",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Export creates a JSON file with all your data\n• Import will replace all existing data\n• Use exports for backup before switching devices",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Export dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Data") },
            text = {
                Column {
                    Text("Copy this data:", modifier = Modifier.padding(bottom = 8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = exportData,
                            modifier = Modifier
                                .padding(8.dp)
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                            fontSize = 10.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Data") },
            text = {
                Column {
                    Text("Paste your backup data:", modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Paste JSON data here...") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (repository.importData(importText)) {
                            Toast.makeText(context, "Data imported successfully!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "Import failed: Invalid data format", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun calculateTimeSinceLastSmoke(lastSmokeTime: LocalDateTime?): String {
    if (lastSmokeTime == null) {
        return "--:--"
    }
    
    val now = LocalDateTime.now()
    val duration = Duration.between(lastSmokeTime, now)
    
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    
    return String.format("%02d:%02d", hours, minutes)
}
