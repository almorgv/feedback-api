package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.User
import com.example.feedback.models.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.security.access.prepost.PreAuthorize
import java.util.Optional

@Suppress("unused")
@RepositoryRestResource
@PreAuthorizeForHead
interface UserRepository : BaseRepository<User> {
    @PreAuthorizeForUser
    override fun findById(id: BaseEntityId): Optional<User>

    @PreAuthorize("hasAnyRole('HEAD', 'ADMIN') or principal.username == #username")
    fun findByUsername(username: String): User?

    @PreAuthorizeForUser
    @Query(
        """
        select u
        from #{#entityName} u
        where u.username = ?#{principal.username}
    """
    )
    fun getCurrentlyAuthenticated(): User?

    @PreAuthorizeForUser
    fun findAllByActiveTrue(pageable: Pageable): Page<User>

    fun findAllByActiveFalse(pageable: Pageable): Page<User>

    @Deprecated("Use findAllByDepartmentAndActiveTrue")
    fun findAllByDepartment(department: String): Iterable<User>

    fun findAllByDepartmentAndActiveTrue(department: String): Iterable<User>

    @RestResource(exported = false)
    fun existsByUserRole(userRole: UserRole): Boolean
}
