package com.example.feedback.models

import org.springframework.data.rest.core.config.Projection
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["name", "job_role_id"])]
)
open class Criteria(
    open var name: String,

    open var description: String,

    @ManyToOne(cascade = [CascadeType.REFRESH])
    open var jobRole: JobRole,

    open var archived: Boolean = false,

    @OneToMany(mappedBy = "criteria", cascade = [CascadeType.REMOVE])
    open var expectations: MutableList<Expectation> = mutableListOf()
) : BaseEntity() {
    override fun toString(): String {
        return "Criteria(name='$name', jobRole='$jobRole') ${super.toString()}"
    }
}

@Projection(types = [Criteria::class])
interface FullCriteriaProjection : BaseEntityProjection {
    fun getName(): String
    fun getDescription(): String
    fun getJobRole(): JobRole
    fun getArchived(): Boolean
    fun getExpectations(): MutableList<Expectation>
}
