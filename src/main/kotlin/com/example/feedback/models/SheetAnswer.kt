package com.example.feedback.models

import javax.persistence.Entity
import javax.persistence.OneToOne

@Entity
open class SheetAnswer(
    @OneToOne
    open var sheet: Sheet,

    open var comment: String? = null,

    open var totalScore: Score? = null,
) : BaseEntity() {
    override fun toString(): String {
        return "SheetAnswer(comment='$comment', totalScore=$totalScore) ${super.toString()}"
    }
}
