package com.example.feedback.models

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.rest.core.config.Projection
import org.springframework.security.core.GrantedAuthority
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PostLoad
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(
    name = "\"user\"",
    uniqueConstraints = [UniqueConstraint(columnNames = ["username"])]
)
open class User(
    open var username: String,

    @Enumerated(EnumType.STRING)
    open var userRole: UserRole = UserRole.ROLE_USER,

    @ManyToOne
    open var jobRole: JobRole? = null,

    @Enumerated(EnumType.STRING)
    open var position: Position = Position.NONE,

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var fullName: String = "",

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var email: String = "",

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var department: String = "",

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var appointment: String = "",

    open var active: Boolean = true,

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var hasCurrentReviews: Boolean = false,

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = [CascadeType.REMOVE, CascadeType.REFRESH])
    open var reviews: MutableList<Review> = mutableListOf(),

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "reviewer")
    open var sheets: MutableList<Sheet> = mutableListOf(),

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
    open var drafts: MutableList<Draft> = mutableListOf(),
) : BaseEntity() {

    @PostLoad
    fun postLoad() {
        hasCurrentReviews = reviews.any { !it.completed }
    }

    override fun toString(): String {
        return "User(username='$username', userRole=$userRole, jobRole=$jobRole, position=$position) ${super.toString()}"
    }
}

enum class Position {
    NONE,
    TRAINEE,
    JUNIOR,
    MIDDLE,
    SENIOR
}

enum class UserRole : GrantedAuthority {
    ROLE_USER,
    ROLE_HEAD,
    ROLE_ADMIN,
    ;

    override fun getAuthority(): String = name
}

@Projection(types = [User::class])
interface MinimalUserProjection : BaseEntityProjection {
    fun getUsername(): String
    fun getUserRole(): UserRole

    @Value("#{target.jobRole?.name}")
    fun getJobRole(): String
    fun getPosition(): Position
    fun getFullName(): String
    fun getEmail(): String
    fun getDepartment(): String
    fun getAppointment(): String
    fun isActive(): Boolean
    fun isHasCurrentReviews(): Boolean
}
