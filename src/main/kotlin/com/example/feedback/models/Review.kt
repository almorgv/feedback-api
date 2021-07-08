package com.example.feedback.models

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection
import org.springframework.security.access.prepost.PostAuthorize
import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.PostLoad
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// TODO persist total scores in DB
@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "period"])]
)
open class Review(
    @ManyToOne(cascade = [CascadeType.REFRESH])
    open var user: User,

    @OneToOne(mappedBy = "review", cascade = [CascadeType.REMOVE, CascadeType.REFRESH])
    open var selfReview: SelfReview? = null,

    open var period: String,

    @OneToMany(mappedBy = "review", cascade = [CascadeType.REMOVE, CascadeType.REFRESH])
    open var sheets: MutableList<Sheet> = mutableListOf(),

    open var completed: Boolean = false,

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var completedDate: Instant? = null,

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Enumerated(EnumType.STRING)
    open var userPosition: Position? = null,

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var criteriaResults: List<CriteriaResult> = listOf(),

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var totalResult: TotalResult? = null,

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var sheetCounters: SheetCounters? = null,
) : BaseEntity() {

    data class ScoreWithWeight(
        val score: Double,
        val weight: Double?,
    )

    private fun calcScoreValue(scores: List<ScoreWithWeight>): Double {
        // if even only one weight isn't specified, sum will be NaN and weights will not be used
        val weightsSum = scores.sumOf { it.weight ?: Double.NaN }.takeUnless { it.isNaN() } ?: 0.0

        val scoreValue =
            if (weightsSum > 0)
                // divide to `answersWeightsSum` for scaling weights to 1.0
                scores.sumOf { it.score * it.weight!! / weightsSum }
            else
                // if we don't use weights, use simple average evaluation
                scores.map { it.score }.average().takeUnless { it.isNaN() } ?: 0.0

        return scoreValue.let { (it * 100).roundToLong().toDouble() / 100 } // round to 2 digits after comma
    }

    // null checks needed because of @PostLoad calls in bidirectional relations
    // and related entities sometimes comes with null values
    @Suppress("SENSELESS_COMPARISON")
    @PostLoad
    fun calcResults() {
        criteriaResults = sheets.asSequence()
            .filter { it.answers != null }
            .flatMap { it.answers }
            .filter { it.score != null }
            .groupBy { it.criteria }
            .map { g ->
                val answers = g.value.filter { it.score != Score.NONE }

                CriteriaResult(
                    criteria = g.key,
                    comments = g.value.mapNotNull { it.comment }.map { Comment(it) },
                    scoreValue = calcScoreValue(
                        answers.map {
                            ScoreWithWeight(score = it.score?.ordinal!!.toDouble(), weight = it.sheet.weight)
                        }
                    ),
                    minScoreValue = answers.mapNotNull { it.score?.ordinal }.minOrNull()?.toDouble() ?: 0.0,
                    maxScoreValue = answers.mapNotNull { it.score?.ordinal }.maxOrNull()?.toDouble() ?: 0.0,
                )
            }

        val sheetsWithScores = sheets.filter { it.avgScoreValue != null && it.avgScoreValue > 0 }

        totalResult = TotalResult(
            comments = sheets.mapNotNull { it.sheetAnswer?.comment }.map { Comment(it) },
            scoreValue = calcScoreValue(
                sheetsWithScores.map { ScoreWithWeight(score = it.avgScoreValue, weight = it.weight) }
            )
        )

        sheetCounters = SheetCounters(
            all = sheets.size,
            filled = sheets.count { it.isFilled && !it.completed },
            completed = sheets.count { it.completed }
        )
    }

    override fun toString(): String {
        return "Review(user=$user, period=$period, completed=$completed, completedDate=$completedDate) ${super.toString()}"
    }
}

data class CriteriaResult(
    val criteria: Criteria,
    val comments: List<Comment>,

    val scoreValue: Double,
    val minScoreValue: Double,
    val maxScoreValue: Double,
    val score: Score = Score.values()[scoreValue.roundToInt()],
    val minScore: Score = Score.values()[minScoreValue.roundToInt()],
    val maxScore: Score = Score.values()[maxScoreValue.roundToInt()],
)

data class TotalResult(
    val comments: List<Comment>,
    val scoreValue: Double,
    val score: Score = Score.values()[scoreValue.roundToInt()],
)

data class Comment(
    val text: String? = null,
)

data class SheetCounters(
    val all: Int,
    val filled: Int,
    val completed: Int,
)

@PostAuthorize(
    """
    hasRole('USER')
    and (returnObject.user.username != principal.username or returnObject.completed == false)
"""
)
@Projection(types = [Review::class])
interface MinimalReviewProjection : BaseEntityProjection {
    fun getUser(): MinimalUserProjection
    fun getSelfReview(): SelfReview?
    fun getPeriod(): String
    fun getCompleted(): Boolean
    fun getCompletedDate(): Instant?
    fun getUserPosition(): Position?
}

@PostAuthorize(
    """
    hasRole('USER')
    and returnObject.completed == true
    and returnObject.user.username == principal.username
"""
)
@Projection(types = [Review::class])
interface CompletedSelfUserReviewProjection : MinimalReviewProjection {
    @Value("#{target.criteriaResults.![{criteria: criteria, score: score, comments: comments}]}")
    fun getCriteriaResults(): List<Map<String, Any>>

    @Value("#{{score: target.totalResult.score, comments: target.totalResult.comments}}")
    fun getTotalResult(): Map<String, Any>
}

@PostAuthorize("hasAnyRole('HEAD', 'ADMIN')")
@Projection(types = [Review::class])
interface FullReviewProjection : MinimalReviewProjection {
    fun getCriteriaResults(): List<CriteriaResult>
    fun getTotalResult(): TotalResult
    fun getSheetCounters(): SheetCounters

    @Value("#{target.sheets.![reviewer]}")
    fun getReviewers(): List<MinimalUserProjection>
}
