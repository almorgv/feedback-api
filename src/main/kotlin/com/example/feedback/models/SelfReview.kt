package com.example.feedback.models

import javax.persistence.Entity
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["review_id"])]
)
open class SelfReview(
    @OneToOne
    open var review: Review,

    open var description: String? = null,

    open var goodThings: String? = null,

    open var badThings: String? = null,
) : BaseEntity() {
    override fun toString(): String {
        return "SelfReview(review=$review) ${super.toString()}"
    }
}
