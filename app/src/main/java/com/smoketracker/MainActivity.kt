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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    var todayTimestamps by remember { mutableStateOf(repository.getTodayTimestamps()) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Calculate average time between vapes
    val avgTimeBetweenVapes = remember(todayTimestamps) {
        val sorted = todayTimestamps.sorted()
        if (sorted.size < 2) {
            null
        } else {
            val intervals = sorted.zipWithNext { prev, next ->
                Duration.between(prev, next).toMinutes().toInt()
            }
            intervals.average().toInt()
        }
    }
    
    // Refresh data when app resumes (e.g., after using widget)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                smokesToday = repository.getSmokesToday()
                lastSmokeTime = repository.getLastSmokeTime()
                todayTimestamps = repository.getTodayTimestamps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
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
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Info Card - flexible size based on available space
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Avg time between vapes section
                if (avgTimeBetweenVapes != null) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Avg time between vapes",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${avgTimeBetweenVapes / 60}h ${avgTimeBetweenVapes % 60}m",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                
                // Vapes today section
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Vapes today",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = smokesToday.toString(),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                // Time since last vape section
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Time since last vape",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timeSinceLastSmoke,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        lastSmokeTime?.let { time ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "at ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Vape button and minus button at the bottom
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    repository.logSmoke()
                    smokesToday = repository.getSmokesToday()
                    lastSmokeTime = repository.getLastSmokeTime()
                    todayTimestamps = repository.getTodayTimestamps()
                    VapeWidgetProvider.requestUpdate(context)
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
                    todayTimestamps = repository.getTodayTimestamps()
                    VapeWidgetProvider.requestUpdate(context)
                },
                enabled = smokesToday > 0,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Reduce count"
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
    val context = LocalContext.current
    
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
                    val sortedTimestamps = todayTimestamps.sortedDescending()
                    items(sortedTimestamps.indices.toList(), key = { sortedTimestamps[it].toString() }) { index ->
                        val timestamp = sortedTimestamps[index]
                        val previousTimestamp = if (index < sortedTimestamps.size - 1) sortedTimestamps[index + 1] else null
                        val timeSincePrevious = if (previousTimestamp != null) {
                            val duration = Duration.between(previousTimestamp, timestamp)
                            val hours = duration.toHours()
                            val minutes = duration.toMinutes() % 60
                            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                        } else {
                            null
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    if (timeSincePrevious != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "after $timeSincePrevious ",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = timestamp.format(timeFormatter),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            repository.removeTimestamp(timestamp)
                                            todayTimestamps = repository.getTodayTimestamps()
                                            VapeWidgetProvider.requestUpdate(context)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
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
                    val leftPadding = 60f
                    val rightPadding = 40f
                    val topPadding = 40f
                    val bottomPadding = 40f
                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding
                    
                    // Draw grid lines and Y-axis labels
                    for (i in 0..4) {
                        val y = topPadding + chartHeight * (1 - i / 4f)
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.1f),
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1f
                        )
                        // Draw Y-axis value labels
                        val yValue = (maxCount * i / 4)
                        drawContext.canvas.nativeCanvas.drawText(
                            yValue.toString(),
                            leftPadding - 10f,
                            y + 8f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }
                    
                    // Draw line chart
                    if (rangeData.isNotEmpty() && rangeData.size > 1) {
                        val points = rangeData.mapIndexed { index, (_, count) ->
                            val x = leftPadding + (chartWidth * index / (rangeData.size - 1).coerceAtLeast(1))
                            val y = topPadding + chartHeight * (1 - count.toFloat() / maxCount)
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
                                leftPadding,
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
                                width - rightPadding,
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Time between vapes chart (today only)
        val todayTimestamps = remember { repository.getTodayTimestamps().sorted() }
        val timeIntervals = remember(todayTimestamps) {
            if (todayTimestamps.size < 2) {
                emptyList()
            } else {
                todayTimestamps.zipWithNext { prev, next ->
                    val duration = Duration.between(prev, next)
                    duration.toMinutes().toInt()
                }
            }
        }
        
        if (timeIntervals.isNotEmpty()) {
            val maxInterval = timeIntervals.maxOrNull() ?: 1
            val avgInterval = timeIntervals.average()
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val primaryColor = MaterialTheme.colorScheme.primary
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            // Get the "end" time of each interval (when the vape happened)
            val intervalEndTimes = todayTimestamps.drop(1).map { it.format(timeFormatter) }
            var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
            var targetMinutesText by remember { mutableStateOf("60") }
            val targetMinutes = targetMinutesText.toIntOrNull() ?: 60
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Time Between Vapes (Today)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Target time input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Target:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = targetMinutesText,
                            onValueChange = { targetMinutesText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "min (${targetMinutes / 60}h ${targetMinutes % 60}m)",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Text(
                        text = "Avg: ${avgInterval.toInt() / 60}h ${avgInterval.toInt() % 60}m",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // Selected bar info
                    if (selectedBarIndex != null) {
                        val idx = selectedBarIndex!!
                        val intervalMins = timeIntervals[idx]
                        Text(
                            text = "@ ${intervalEndTimes[idx]} — ${intervalMins / 60}h ${intervalMins % 60}m since previous",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        Text(
                            text = "Tap a bar to see details",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    var chartWidth by remember { mutableStateOf(0f) }
                    var leftPadding by remember { mutableStateOf(60f) }
                    var barSpacing by remember { mutableStateOf(0f) }
                    
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .pointerInput(timeIntervals) {
                                detectTapGestures { offset ->
                                    if (barSpacing > 0) {
                                        val tappedIndex = ((offset.x - leftPadding) / barSpacing).toInt()
                                        if (tappedIndex in timeIntervals.indices) {
                                            selectedBarIndex = if (selectedBarIndex == tappedIndex) null else tappedIndex
                                        }
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        leftPadding = 60f
                        val rightPadding = 20f
                        val topPadding = 20f
                        val bottomPadding = 40f
                        chartWidth = width - leftPadding - rightPadding
                        val chartHeight = height - topPadding - bottomPadding
                        val barWidthLocal = (chartWidth / timeIntervals.size) * 0.7f
                        barSpacing = chartWidth / timeIntervals.size
                        
                        // Draw grid lines and Y-axis labels (in minutes)
                        val ySteps = 4
                        for (i in 0..ySteps) {
                            val y = topPadding + chartHeight * (1 - i / ySteps.toFloat())
                            drawLine(
                                color = onSurfaceColor.copy(alpha = 0.1f),
                                start = Offset(leftPadding, y),
                                end = Offset(width - rightPadding, y),
                                strokeWidth = 1f
                            )
                            val yValueMinutes = (maxInterval * i / ySteps)
                            val label = if (yValueMinutes >= 60) {
                                "${yValueMinutes / 60}h${if (yValueMinutes % 60 > 0) " ${yValueMinutes % 60}m" else ""}"
                            } else {
                                "${yValueMinutes}m"
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                leftPadding - 10f,
                                y + 8f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                            )
                        }
                        
                        // Draw bars
                        timeIntervals.forEachIndexed { index, intervalMinutes ->
                            val barHeight = (intervalMinutes.toFloat() / maxInterval) * chartHeight
                            val x = leftPadding + (index * barSpacing) + (barSpacing - barWidthLocal) / 2
                            val y = topPadding + chartHeight - barHeight
                            
                            val isSelected = selectedBarIndex == index
                            drawRect(
                                color = if (isSelected) tertiaryColor else tertiaryColor.copy(alpha = 0.7f),
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(barWidthLocal, barHeight)
                            )
                            
                            // Draw bar number below
                            drawContext.canvas.nativeCanvas.drawText(
                                "#${index + 1}",
                                x + barWidthLocal / 2,
                                height - 10f,
                                android.graphics.Paint().apply {
                                    color = if (isSelected) android.graphics.Color.DKGRAY else android.graphics.Color.GRAY
                                    textSize = 20f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                        
                        // Draw average line
                        val avgY = topPadding + chartHeight - (avgInterval.toFloat() / maxInterval) * chartHeight
                        drawLine(
                            color = tertiaryColor,
                            start = Offset(leftPadding, avgY),
                            end = Offset(width - rightPadding, avgY),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        
                        // Draw target line
                        val targetY = topPadding + chartHeight - (targetMinutes.toFloat() / maxInterval) * chartHeight
                        if (targetY >= topPadding && targetY <= topPadding + chartHeight) {
                            drawLine(
                                color = primaryColor,
                                start = Offset(leftPadding, targetY),
                                end = Offset(width - rightPadding, targetY),
                                strokeWidth = 3f
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Time interval stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val longestInterval = timeIntervals.maxOrNull() ?: 0
                val shortestInterval = timeIntervals.minOrNull() ?: 0
                
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
                        Text("Longest Gap", fontSize = 12.sp)
                        Text(
                            text = "${longestInterval / 60}h ${longestInterval % 60}m",
                            fontSize = 20.sp,
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
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Shortest Gap", fontSize = 12.sp)
                        Text(
                            text = "${shortestInterval / 60}h ${shortestInterval % 60}m",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Time Between Vapes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Need at least 2 vapes today to show intervals",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
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
