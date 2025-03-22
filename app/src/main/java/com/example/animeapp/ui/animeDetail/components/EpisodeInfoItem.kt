package com.example.animeapp.ui.animeDetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EpisodeInfoItem(
    text: String,
    color: Color,
    icon: ImageVector,
    isFirst: Boolean,
    isLast: Boolean,
    hasRight: Boolean
) {
    val leftShape = if (isFirst) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)
    val rightShape =
        if (isLast || !hasRight) RoundedCornerShape(percent = 50) else RoundedCornerShape(0.dp)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(
                topStart = leftShape.topStart,
                topEnd = rightShape.topEnd,
                bottomStart = leftShape.bottomStart,
                bottomEnd = rightShape.bottomEnd
            ),
            color = color,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}