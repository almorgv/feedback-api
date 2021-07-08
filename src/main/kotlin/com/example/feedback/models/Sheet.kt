package com.example.feedback.models

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.rest.core.config.Projection
import org.springframework.security.access.prepost.PreAuthorize
import java.time.Instant
import java.time.LocalDate
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

// TODO add ability to make sheet read only for user
@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["review_id", "reviewer_id"])]
)
open class Sheet(
    @ManyToOne
    open var review: Review,

    @ManyToOne
    open var reviewer: User,

    open var dueDate: LocalDate,

    open var completed: Boolean = false,

    open var weight: Double? = null,

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var completedDate: Instant? = null,

    @Enumerated(EnumType.STRING)
    open var reviewerGroup: ReviewerGroup = ReviewerGroup.COLLEAGUE,

    @OneToOne(cascade = [CascadeType.REMOVE, CascadeType.REFRESH])
    open var sheetAnswer: SheetAnswer? = null,

    @OneToMany(mappedBy = "sheet", cascade = [CascadeType.REMOVE, CascadeType.REFRESH])
    open var answers: MutableList<Answer> = mutableListOf(),

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var isFilled: Boolean = false,

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var avgScoreValue: Double = 0.0,

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var avgScore: Score = Score.NONE,
) : BaseEntity() {

    // null checks needed because this @PostLoad used in bidirectional relations
    // and related entities may come not filled with null values
    @Suppress("SENSELESS_COMPARISON")
    @PostLoad
    fun postLoad() {
        isFilled = answers.all {
            it.score != null && (it.comment != null || it.score == Score.NONE)
        } &&
            sheetAnswer?.comment != null && sheetAnswer?.totalScore != null

        // TODO try to set weight for sheetAnswer.totalScore
        avgScoreValue = answers
            .asSequence()
            .map { it.score?.ordinal }
            .plus(sheetAnswer?.totalScore?.ordinal)
            .filterNotNull()
            .filter { it > 0 }
            .average()
            .takeUnless {
                it.isNaN()
            }
            ?: 0.0

        avgScore = Score.values()[avgScoreValue.roundToInt()]
    }

    override fun toString(): String {
        return "Sheet(" +
            "review=$review, " +
            "reviewer=$reviewer, " +
            "dueDate=$dueDate, " +
            "completed=$completed, " +
            "weight=$weight, " +
            "completedDate=$completedDate, " +
            "reviewerGroup=$reviewerGroup, " +
            "sheetAnswer=$sheetAnswer, " +
            "avgScoreValue=$avgScoreValue, " +
            "avgScore=$avgScore" +
            ") " +
            super.toString()
    }
}

enum class ReviewerGroup {
    STAKEHOLDER,
    COLLEAGUE,
    MENTEE,
    MENTOR,
    MANAGER,
    PROJECT_MANAGER,
}

@PreAuthorize("hasRole('USER')")
@Projection(types = [Sheet::class])
interface MinimalSheetProjection : BaseEntityProjection {
    fun getReview(): MinimalReviewProjection
    fun getReviewer(): MinimalUserProjection
    fun getDueDate(): LocalDate
    fun getWeight(): Double?
    fun getCompleted(): Boolean
    fun getCompletedDate(): Instant?
    fun getReviewerGroup(): ReviewerGroup
    fun isFilled(): Boolean
}

@PreAuthorize("hasAnyRole('HEAD', 'ADMIN')")
@Projection(types = [Sheet::class])
interface FullSheetProjection : MinimalSheetProjection {
    fun getAvgScoreValue(): Double
    fun getAvgScore(): Score
}
