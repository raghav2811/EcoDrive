package com.ecodrive.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecodrive.app.utils.EcoScoreUtils
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EcoScoreGauge(
    score: Float,
    modifier: Modifier = Modifier
) {
    var animatedScore by remember { mutableStateOf(0f) }
    
    LaunchedEffect(score) {
        animate(
            initialValue = animatedScore,
            targetValue = score,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animatedScore = value
        }
    }
    
    val scoreColor = Color(EcoScoreUtils.getEcoScoreColor(animatedScore))
    val displayColor = if (animatedScore == 0f) {
        MaterialTheme.colorScheme.outline  // Gray for no data
    } else {
        scoreColor
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val radius = canvasWidth / 2 - 20.dp.toPx()
                
                // Background arc
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(
                        (canvasWidth - radius * 2) / 2,
                        (canvasHeight - radius * 2) / 2
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Score arc
                val sweepAngle = (animatedScore / 100f) * 270f
                drawArc(
                    color = displayColor,
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(
                        (canvasWidth - radius * 2) / 2,
                        (canvasHeight - radius * 2) / 2
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (animatedScore == 0f) "--" else "${animatedScore.toInt()}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = displayColor
                )
                Text(
                    text = "ECO SCORE",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when {
                animatedScore == 0f -> "Waiting to start..."
                animatedScore >= 80 -> "Excellent"
                animatedScore >= 60 -> "Good"
                animatedScore >= 40 -> "Fair"
                else -> "Needs Improvement"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = displayColor
        )
    }
}
