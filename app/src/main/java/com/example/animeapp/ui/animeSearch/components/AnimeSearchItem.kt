package com.example.animeapp.ui.animeSearch.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Score
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.animeDetailPlaceholder
import com.example.animeapp.ui.common_ui.AsyncImageWithPlaceholder
import com.example.animeapp.ui.common_ui.DataTextWithIcon
import com.example.animeapp.ui.common_ui.FilterChipView

@Preview
@Composable
fun AnimeSearchItemPreview() {
    AnimeSearchItem(
        anime = animeDetailPlaceholder,
        onItemClick = {}
    )
}

@Composable
fun AnimeSearchItem(
    anime: AnimeDetail?,
    onGenreClick: ((String) -> Unit)? = null,
    onItemClick: ((Int) -> Unit)? = null,
) {
    anime?.let { data ->
        Column(
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
                .clickable { onItemClick?.invoke(data.mal_id) }
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AsyncImageWithPlaceholder(
                    model = data.images.jpg.image_url,
                    contentDescription = data.title,
                    isAiring = data.airing
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                ) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${data.type ?: "Unknown Type"} - ${data.aired.prop.from.year ?: "Unknown Year"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (data.approved) {
                            Icon(
                                imageVector = Icons.Filled.Recommend,
                                contentDescription = "Approved",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    data.genres?.let { genres ->
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genres.map { it.name }.toList().forEach { data ->
                                FilterChipView(data) {
                                    onGenreClick?.invoke(data)
                                }
                            }
                        }
                    }
                    DataTextWithIcon(
                        label = "Score",
                        value = data.score.toString(),
                        icon = Icons.Filled.Score
                    )
                    DataTextWithIcon(
                        label = "Rank",
                        value = data.rank.toString(),
                        icon = Icons.Filled.Star
                    )
                    DataTextWithIcon(
                        label = "Popularity",
                        value = data.popularity.toString(),
                        icon = Icons.Filled.People
                    )
                    DataTextWithIcon(
                        label = "Members",
                        value = data.members.toString(),
                        icon = Icons.Filled.People
                    )
                }
            }
        }
    }
}