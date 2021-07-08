package com.example.feedback.models

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["name"])]
)
open class JobRole(
    open var name: String,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "jobRole")
    open var users: MutableSet<User> = mutableSetOf(),

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "jobRole", cascade = [CascadeType.REMOVE])
    open var criterias: MutableSet<Criteria> = mutableSetOf(),
) : BaseEntity() {
    override fun toString(): String {
        return "JobRole(name='$name')"
    }
}
