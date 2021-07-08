package com.example.feedback.models

import org.springframework.data.rest.core.config.Projection
import org.springframework.security.access.prepost.PostAuthorize
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "author_id"])])
open class Draft(
    @ManyToOne(cascade = [CascadeType.REFRESH])
    open var user: User,

    @ManyToOne(cascade = [CascadeType.REFRESH])
    open var author: User,

    open var text: String,
): BaseEntity() {
    override fun toString(): String {
        return "Draft(user=${user}, author=${author}, text=\"${text}\") ${super.toString()}"
    }
}

@PostAuthorize(
    """
    returnObject.author.username == principal.username
    """
)
@Projection(types = [Draft::class])
interface DraftProjection : BaseEntityProjection {
    fun getUser(): MinimalUserProjection
    fun getText(): String
}
