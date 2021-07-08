package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.Review
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.security.access.prepost.PostAuthorize
import java.util.Optional

@RepositoryRestResource
@PreAuthorizeForHead
interface ReviewRepository : BaseRepository<Review> {
    @PreAuthorizeForUser
    @PostAuthorize("hasAnyRole('HEAD', 'ADMIN') or returnObject.orElse(null)?.user?.username == principal.username")
    override fun findById(id: BaseEntityId): Optional<Review>

    fun findAllByCompletedFalse(pageable: Pageable): Page<Review>
    fun findAllByCompletedTrue(pageable: Pageable): Page<Review>
}
