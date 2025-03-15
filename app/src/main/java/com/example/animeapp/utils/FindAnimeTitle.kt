package com.example.animeapp.utils

import android.util.Log
import com.example.animeapp.models.AnimeAniwatch
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail
import org.apache.commons.text.similarity.LevenshteinDistance
import java.util.regex.Pattern

object FindAnimeTitle {

    private val levenshteinDistance = LevenshteinDistance.getDefaultInstance()
    private const val MIN_SIMILARITY_THRESHOLD = 0.25
    private val titleRegex = Pattern.compile(
        "^(.*?)(?:\\s+(?:(?:Season|Movie)\\s*(\\d+)|(\\d+)(?:st|nd|rd|th)?\\s*(?:Season)?|(\\d+)))?\\s*(?:\\(?(Part\\s*\\d+)\\)?)?\\s*$",
        Pattern.CASE_INSENSITIVE
    )
    private val numberRegex = Pattern.compile("\\d+")

    fun findClosestAnimes(
        animeSearchData: AnimeAniwatchSearchResponse,
        animeDetail: AnimeDetail?
    ): List<AnimeAniwatch> {
        if (animeDetail == null) {
            return animeSearchData.animes.sortedBy { it.name.normalizeForComparison() }.take(2)
        }

        val normalizedDetailTitles = listOfNotNull(
            animeDetail.title.normalizeForComparison(),
            animeDetail.title_english?.normalizeForComparison()
        ) + (animeDetail.title_synonyms?.map { it.normalizeForComparison() } ?: emptyList())

        val scoredAnimes = animeSearchData.animes.map { anime ->
            val normalizedAnimeName = anime.name.normalizeForComparison()
            Log.d("FindAnimeTitle", "Comparing '${anime.name}' with '${animeDetail.title}'")

            val bestScore = normalizedDetailTitles.maxOf { detailTitle ->
                calculateEnhancedSimilarity(normalizedAnimeName, detailTitle)
            }

            Log.d("FindAnimeTitle", "Best score for '${anime.name}': $bestScore")
            ScoredAnime(anime, bestScore)
        }

        return scoredAnimes.sortedByDescending { it.score }
            .filter { it.score >= MIN_SIMILARITY_THRESHOLD }
            .take(2)
            .map { it.anime }
    }

    private fun extractCoreTitleAndNumber(title: String): Pair<String, Pair<Int?, String?>> {
        val modifiedTitle = title.trim()
        val matcher = titleRegex.matcher(modifiedTitle)
        var seasonNumber: Int? = null
        var coreTitle = modifiedTitle
        var part: String? = null

        if (matcher.find()) {
            coreTitle = matcher.group(1)?.trim() ?: ""
            seasonNumber = (2..4).firstNotNullOfOrNull {
                matcher.group(it)?.toIntOrNull()
            }
            part = matcher.group(5)?.trim()
        } else {
            val numberMatcher = numberRegex.matcher(modifiedTitle)
            if (numberMatcher.find()) {
                seasonNumber = numberMatcher.group().toIntOrNull()
                coreTitle = modifiedTitle.replace(numberMatcher.group(), "").trim()
            }
        }

        if (seasonNumber == null) {
            val numbers =
                numberRegex.toRegex().findAll(modifiedTitle).map { it.value.toInt() }.toList()
            seasonNumber = if (numbers.isNotEmpty() && numbers.last() in 1900..2100) {
                null
            } else {
                numbers.lastOrNull()?.also {
                    coreTitle = modifiedTitle.replace(it.toString(), "").trim()
                }
            }
            if (seasonNumber == null && coreTitle.lowercase().contains("movie")) {
                seasonNumber = 1
            }
        }

        return coreTitle to (seasonNumber to part)
    }

    private fun calculateEnhancedSimilarity(s1: String, s2: String): Double {
        val (coreTitle1, numberPair1) = extractCoreTitleAndNumber(s1)
        val (coreTitle2, numberPair2) = extractCoreTitleAndNumber(s2)
        val (number1, part1) = numberPair1
        val (number2, part2) = numberPair2

        println("Core titles: '$coreTitle1' <=> '$coreTitle2', Numbers: $number1 <=> $number2, Parts: $part1 <=> $part2")

        val coreSimilarity = calculateCombinedSimilarity(coreTitle1, coreTitle2)

        var score = coreSimilarity * 0.6
        var numberScore = 0.0
        var partScore = 0.0

        if (number1 != null && number2 != null && number1 == number2) {
            numberScore = 0.2
            if (part1 != null && part2 != null && part1 == part2) {
                partScore = 0.2
            } else if (part1 != null || part2 != null) {
                score *= 0.95
            }
            if (coreSimilarity < MIN_SIMILARITY_THRESHOLD) {
                numberScore *= 0.5
                partScore *= 0.5
            }
        } else if (number1 != null || number2 != null) {
            score *= 0.9
        }

        return (score + numberScore + partScore).coerceAtMost(1.0)
    }

    private fun calculateCombinedSimilarity(s1: String, s2: String): Double =
        (calculateLevenshteinSimilarity(s1, s2) * 0.7 + calculateJaccardSimilarity(s1, s2) * 0.3)
            .coerceAtMost(1.0)

    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance.apply(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return 1.0 - distance.toDouble() / maxLength
    }

    private fun calculateJaccardSimilarity(s1: String, s2: String): Double {
        val set1 = s1.split("\\s+").toSet()
        val set2 = s2.split("\\s+").toSet()
        val intersection = set1.intersect(set2)
        val union = set1.union(set2)
        return if (union.isNotEmpty()) intersection.size.toDouble() / union.size.toDouble() else 0.0
    }

    private data class ScoredAnime(val anime: AnimeAniwatch, val score: Double)

    private fun String.normalizeForComparison(): String =
        replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().lowercase()
}