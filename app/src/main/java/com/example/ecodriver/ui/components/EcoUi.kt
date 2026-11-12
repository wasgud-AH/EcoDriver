package com.example.ecodriver.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecodriver.ui.theme.Amber
import com.example.ecodriver.ui.theme.ButtonGreen
import com.example.ecodriver.ui.theme.CardBg
import com.example.ecodriver.ui.theme.TextPrimary
import com.example.ecodriver.ui.theme.TextSecondary

val ScreenPadding = PaddingValues(24.dp)
val CardShape = RoundedCornerShape(16.dp)
private val DarkOnGreen = Color(0xFF002A02)

@Composable
fun EcoTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun EcoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .background(CardBg, CardShape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    EcoCard(modifier) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GreenButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen, contentColor = DarkOnGreen),
    ) { Text(text) }
}

@Composable
fun WaitingBanner(modifier: Modifier = Modifier) {
    Text(
        "Waiting for car",
        color = Color(0xFF1A1200),
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .fillMaxWidth()
            .background(Amber, CardShape)
            .padding(16.dp),
    )
}

@Composable
fun ConfirmDialog(title: String, body: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmText, color = Amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextPrimary) } },
    )
}

@Composable
fun ecoFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = ButtonGreen,
    unfocusedBorderColor = TextSecondary,
    focusedLabelColor = ButtonGreen,
    unfocusedLabelColor = TextSecondary,
    cursorColor = ButtonGreen,
)

/**
 * Line chart over a shared x range. Points with a null y leave a gap.
 * markers are x values drawn as dots on the line.
 */
@Composable
fun LineChart(
    xs: List<Float>,
    ys: List<Float?>,
    modifier: Modifier = Modifier,
    color: Color = ButtonGreen,
    xRange: ClosedFloatingPointRange<Float>? = null,
    yMin: Float = 0f,
    yMax: Float? = null,
    markers: List<Float> = emptyList(),
    markerColor: Color = Amber,
) {
    Canvas(modifier) {
        val pts = xs.zip(ys)
        if (pts.size < 2) return@Canvas
        val x0 = xRange?.start ?: xs.first()
        val x1 = xRange?.endInclusive ?: xs.last()
        val top = yMax ?: (ys.filterNotNull().maxOrNull() ?: yMin)
        if (x1 <= x0) return@Canvas
        val span = (top - yMin).takeIf { it > 0f } ?: 1f // flat data draws along the bottom
        fun px(x: Float) = (x - x0) / (x1 - x0) * size.width
        fun py(y: Float) = size.height - ((y - yMin) / span).coerceIn(0f, 1f) * size.height

        val path = Path()
        var drawing = false
        for ((x, y) in pts) {
            if (y == null) { drawing = false; continue }
            if (drawing) path.lineTo(px(x), py(y)) else path.moveTo(px(x), py(y))
            drawing = true
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))

        for (m in markers) {
            // marker sits on the nearest point's y
            val nearest = pts.minByOrNull { kotlin.math.abs(it.first - m) }?.second ?: continue
            drawCircle(markerColor, radius = 4.dp.toPx(), center = Offset(px(m), py(nearest)))
        }
    }
}
