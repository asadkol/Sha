package com.shahpharmacy.app.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.shahpharmacy.app.data.model.DailySale
import com.shahpharmacy.app.ui.theme.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class RechartsDataPoint(
    val day: Int,
    val dayLabel: String,
    val date: String,
    val revenue: Double,
    val formattedRevenue: String
)

@Composable
fun DailySalesRechartsReport(
    sales: List<DailySale>,
    modifier: Modifier = Modifier,
    onAddSampleSales: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var useRechartsWebView by remember { mutableStateOf(true) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Generate full days array for the selected month
    val daysInMonth = selectedMonth.lengthOfMonth()
    val monthPrefix = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val monthTitle = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

    val monthDataPoints = remember(sales, selectedMonth) {
        val list = mutableListOf<RechartsDataPoint>()
        for (day in 1..daysInMonth) {
            val dateStr = String.format(Locale.US, "%s-%02d", monthPrefix, day)
            val sale = sales.find { it.date == dateStr }
            val amount = sale?.amount ?: 0.0
            val dayLabel = "${day}${getDaySuffix(day)}"
            list.add(
                RechartsDataPoint(
                    day = day,
                    dayLabel = dayLabel,
                    date = dateStr,
                    revenue = amount,
                    formattedRevenue = formatMoney(amount)
                )
            )
        }
        list
    }

    val totalMonthlyRevenue = remember(monthDataPoints) { monthDataPoints.sumOf { it.revenue } }
    val activeDaysCount = remember(monthDataPoints) { monthDataPoints.count { it.revenue > 0 } }
    val dailyAverage = remember(totalMonthlyRevenue, activeDaysCount, daysInMonth) {
        if (activeDaysCount > 0) totalMonthlyRevenue / activeDaysCount else 0.0
    }
    val peakDataPoint = remember(monthDataPoints) {
        monthDataPoints.maxByOrNull { it.revenue }?.takeIf { it.revenue > 0 }
    }

    val rechartsHtml = remember(monthDataPoints, monthTitle, totalMonthlyRevenue) {
        generateRechartsHtml(monthDataPoints, monthTitle, totalMonthlyRevenue)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_sales_recharts_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Daily Sales Trends",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NavyPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BlueLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Recharts",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueSecondary
                            )
                        }
                    }
                    Text(
                        text = "Revenue performance for $monthTitle",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { selectedMonth = selectedMonth.minusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("◀", fontSize = 12.sp, color = NavyPrimary)
                    }
                    Text(
                        text = selectedMonth.format(DateTimeFormatter.ofPattern("MMM yy")),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("▶", fontSize = 12.sp, color = NavyPrimary)
                    }
                }
            }

            // Monthly Summary KPI Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = BlueLight),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Revenue", fontSize = 10.sp, color = BlueSale)
                        Text(
                            text = formatMoney(totalMonthlyRevenue),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = NavyPrimary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = GreenLight),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Daily Average", fontSize = 10.sp, color = GreenCash)
                        Text(
                            text = formatMoney(dailyAverage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GreenCash
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = OrangeLight),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Peak Day", fontSize = 10.sp, color = OrangePurchase)
                        Text(
                            text = peakDataPoint?.let { "${it.dayLabel}: ${formatMoney(it.revenue)}" } ?: "None",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = OrangePurchase,
                            maxLines = 1
                        )
                    }
                }
            }

            // Visualizer Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (useRechartsWebView) NavyPrimary else Color.Transparent)
                            .clickable { useRechartsWebView = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "⚡ Recharts (Interactive)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (useRechartsWebView) Color.White else TextMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!useRechartsWebView) NavyPrimary else Color.Transparent)
                            .clickable { useRechartsWebView = false }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "📱 Native Curve Chart",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!useRechartsWebView) Color.White else TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Recharts_Report.html", rechartsHtml))
                        Toast.makeText(context, "Recharts HTML report copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share / Copy Recharts HTML",
                        tint = BlueSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Visualization Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                if (useRechartsWebView) {
                    RechartsWebView(htmlContent = rechartsHtml)
                } else {
                    NativeSalesTrendCanvas(
                        dataPoints = monthDataPoints,
                        selectedIndex = selectedPointIndex,
                        onSelectIndex = { selectedPointIndex = it }
                    )
                }
            }

            // Legend / Insights Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary)
                    )
                    Text("Daily Sales Trend", fontSize = 11.sp, color = TextMuted)
                }

                Text(
                    text = "$activeDaysCount active sales days in $monthTitle",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BlueSale
                )
            }

            if (activeDaysCount == 0 && onAddSampleSales != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = BlueLight
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "No sales logged for this month yet.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Button(
                            onClick = onAddSampleSales,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Load Demo Trend Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Selected Day Callout (when user taps on a point in native chart)
            selectedPointIndex?.let { idx ->
                val pt = monthDataPoints.getOrNull(idx)
                if (pt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BlueLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Selected Day: ${pt.date} (${pt.dayLabel})", fontSize = 11.sp, color = TextMuted)
                                Text(pt.formattedRevenue, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                            }
                            TextButton(onClick = { selectedPointIndex = null }) {
                                Text("Dismiss", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RechartsWebView(htmlContent: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    displayZoomControls = false
                    builtInZoomControls = false
                }
                webViewClient = WebViewClient()
                setBackgroundColor(0x00000000)
                loadDataWithBaseURL("https://app.shahpharmacy", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://app.shahpharmacy", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun NativeSalesTrendCanvas(
    dataPoints: List<RechartsDataPoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    if (dataPoints.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sales data available for this month", color = TextMuted, fontSize = 12.sp)
        }
        return
    }

    val maxRevenue = remember(dataPoints) {
        val max = dataPoints.maxOfOrNull { it.revenue } ?: 1.0
        if (max > 0) max else 1.0
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 28.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
            .pointerInput(dataPoints) {
                detectTapGestures { offset ->
                    val width = size.width
                    val step = width / (dataPoints.size - 1).coerceAtLeast(1)
                    val tappedIndex = (offset.x / step).toInt().coerceIn(0, dataPoints.size - 1)
                    onSelectIndex(tappedIndex)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val count = dataPoints.size
        val stepX = width / (count - 1).coerceAtLeast(1)

        // Draw horizontal grid lines (4 lines)
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = height * (i.toFloat() / gridLines)
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Build curve path
        val path = Path()
        val fillPath = Path()

        val points = dataPoints.mapIndexed { index, dp ->
            val x = index * stepX
            val normalized = (dp.revenue / maxRevenue).toFloat().coerceIn(0f, 1f)
            val y = height - (normalized * height)
            Offset(x, y)
        }

        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, height)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                val controlX1 = p0.x + (p1.x - p0.x) / 2
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) / 2
                val controlY2 = p1.y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, height)
            fillPath.close()

            // Draw Area Fill Gradient (Recharts style)
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NavyPrimary.copy(alpha = 0.35f),
                        BlueSecondary.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )

            // Draw Area Stroke
            drawPath(
                path = path,
                color = NavyPrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Data points
            points.forEachIndexed { idx, point ->
                val isSelected = selectedIndex == idx
                val hasRevenue = dataPoints[idx].revenue > 0

                if (hasRevenue || isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = (if (isSelected) 6.dp else 4.dp).toPx(),
                        center = point
                    )
                    drawCircle(
                        color = if (isSelected) OrangePurchase else BlueSecondary,
                        radius = (if (isSelected) 4.5.dp else 2.5.dp).toPx(),
                        center = point
                    )
                }
            }
        }
    }
}

private fun getDaySuffix(n: Int): String {
    if (n in 11..13) return "th"
    return when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

private fun generateRechartsHtml(
    data: List<RechartsDataPoint>,
    monthTitle: String,
    totalMonthlyRevenue: Double
): String {
    val jsonFormat = Json { encodeDefaults = true }
    val dataJson = jsonFormat.encodeToString(data)

    return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Shah Pharmacy - Daily Sales Trends</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      background: transparent;
      color: #1e293b;
      padding: 8px 4px 4px 4px;
      overflow: hidden;
      user-select: none;
      -webkit-user-select: none;
    }
    #chart-container {
      width: 100%;
      height: 240px;
    }
    .recharts-default-tooltip {
      background-color: #0B3D91 !important;
      border: none !important;
      border-radius: 8px !important;
      box-shadow: 0 4px 14px rgba(11, 61, 145, 0.25) !important;
      padding: 8px 12px !important;
    }
    .recharts-tooltip-label {
      color: #93C5FD !important;
      font-weight: 700 !important;
      font-size: 11px !important;
      margin-bottom: 2px !important;
    }
    .recharts-tooltip-item {
      color: #ffffff !important;
      font-size: 13px !important;
      font-weight: 600 !important;
      padding: 0 !important;
    }
    /* Fallback SVG chart when CDN is loading or offline */
    .offline-svg-chart {
      width: 100%;
      height: 100%;
    }
  </style>
  <!-- React, ReactDOM, PropTypes and Recharts from unpkg -->
  <script src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
  <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
  <script src="https://unpkg.com/prop-types@15.8.1/prop-types.min.js"></script>
  <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>
</head>
<body>
  <div id="chart-container"></div>

  <script>
    const salesData = $dataJson;

    function renderRecharts() {
      if (window.Recharts && window.React && window.ReactDOM) {
        const e = React.createElement;
        const { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip } = window.Recharts;

        const App = () => {
          return e(ResponsiveContainer, { width: '100%', height: 235 },
            e(AreaChart, { data: salesData, margin: { top: 10, right: 12, left: -18, bottom: 0 } },
              e('defs', null,
                e('linearGradient', { id: 'salesGradient', x1: '0', y1: '0', x2: '0', y2: '1' },
                  e('stop', { offset: '0%', stopColor: '#0B3D91', stopOpacity: 0.75 }),
                  e('stop', { offset: '60%', stopColor: '#1167C9', stopOpacity: 0.20 }),
                  e('stop', { offset: '100%', stopColor: '#1167C9', stopOpacity: 0.0 })
                )
              ),
              e(CartesianGrid, { strokeDasharray: '3 3', stroke: '#E2E8F0', vertical: false }),
              e(XAxis, {
                dataKey: 'day',
                stroke: '#64748B',
                fontSize: 10,
                tickLine: false,
                interval: 2,
                tickFormatter: (d) => d
              }),
              e(YAxis, {
                stroke: '#64748B',
                fontSize: 9,
                tickLine: false,
                tickFormatter: (val) => val >= 1000 ? (val / 1000).toFixed(0) + 'k' : val
              }),
              e(Tooltip, {
                formatter: (val) => ['Rs. ' + Number(val).toLocaleString(), 'Daily Revenue'],
                labelFormatter: (label) => 'Day ' + label + ' of $monthTitle'
              }),
              e(Area, {
                type: 'monotone',
                dataKey: 'revenue',
                stroke: '#0B3D91',
                strokeWidth: 2.5,
                fillOpacity: 1,
                fill: 'url(#salesGradient)',
                dot: false,
                activeDot: { r: 5, fill: '#1167C9', stroke: '#ffffff', strokeWidth: 2 }
              })
            )
          );
        };

        ReactDOM.render(e(App), document.getElementById('chart-container'));
      } else {
        renderSvgFallback();
      }
    }

    function renderSvgFallback() {
      const container = document.getElementById('chart-container');
      const maxVal = Math.max(...salesData.map(d => d.revenue), 1);
      const points = salesData.map((d, i) => {
        const x = (i / (salesData.length - 1)) * 340 + 20;
        const y = 180 - (d.revenue / maxVal) * 150;
        return x + ',' + y;
      }).join(' ');

      const areaPoints = '20,180 ' + points + ' 360,180';

      container.innerHTML = `
        <svg viewBox="0 0 380 200" class="offline-svg-chart">
          <defs>
            <linearGradient id="fallbackGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#0B3D91" stop-opacity="0.4"/>
              <stop offset="100%" stop-color="#1167C9" stop-opacity="0.0"/>
            </linearGradient>
          </defs>
          <line x1="20" y1="30" x2="360" y2="30" stroke="#E2E8F0" stroke-width="1" stroke-dasharray="3,3"/>
          <line x1="20" y1="80" x2="360" y2="80" stroke="#E2E8F0" stroke-width="1" stroke-dasharray="3,3"/>
          <line x1="20" y1="130" x2="360" y2="130" stroke="#E2E8F0" stroke-width="1" stroke-dasharray="3,3"/>
          <line x1="20" y1="180" x2="360" y2="180" stroke="#CBD5E1" stroke-width="1"/>
          <polygon points="` + areaPoints + `" fill="url(#fallbackGrad)" />
          <polyline fill="none" stroke="#0B3D91" stroke-width="2.5" stroke-linecap="round" points="` + points + `" />
        </svg>
      `;
    }

    // Try rendering Recharts or fallback
    if (document.readyState === 'complete') {
      renderRecharts();
    } else {
      window.addEventListener('load', renderRecharts);
    }
    // Failover timer if CDN script fails or device is offline
    setTimeout(() => {
      if (!window.Recharts) renderSvgFallback();
    }, 1500);
  </script>
</body>
</html>
    """.trimIndent()
}
