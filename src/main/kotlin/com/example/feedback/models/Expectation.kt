package com.example.feedback.models

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["criteria_id", "position"])]
)
open class Expectation(
    @ManyToOne
    open var criteria: Criteria,

    @Enumerated(EnumType.STRING)
    open var position: Position,

    open var description: String,
) : BaseEntity() {
    override fun toString(): String {
        return "Expectation(criteria=$criteria, position=$position) ${super.toString()}"
    }
}
