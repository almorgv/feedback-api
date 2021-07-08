package com.example.feedback.models

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.rest.core.config.Projection
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["sheet_id", "criteria_id"])]
)
open class Answer(
    @ManyToOne(cascade = [CascadeType.REFRESH])
    open var sheet: Sheet,

    @ManyToOne(cascade = [CascadeType.REFRESH])
    open var criteria: Criteria,

    open var score: Score? = null,

    open var comment: String? = null,

    @Deprecated("")
    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var isFilled: Boolean = false,
) : BaseEntity() {
    override fun toString(): String {
        return "Answer(criteria=$criteria, score=$score, comment=$comment) ${super.toString()}"
    }
}

enum class Score {
    NONE,
    WAY_BELOW_EXPECTATIONS,
    BELOW_EXPECTATIONS,
    MEET_EXPECTATIONS,
    ABOVE_EXPECTATIONS,
    WAY_ABOVE_EXPECTATIONS,
}

@Projection(types = [Answer::class])
interface AnswerProjectionWithCriteria : BaseEntityProjection {
    fun getCriteria(): FullCriteriaProjection
    fun getScore(): Score?
    fun getComment(): String?

    @Deprecated("")
    fun getIsFilled(): Boolean
}
