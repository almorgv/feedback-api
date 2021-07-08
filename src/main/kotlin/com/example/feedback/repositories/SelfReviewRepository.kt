package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.SelfReview
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import java.util.Optional

@RepositoryRestResource
@PreAuthorizeForHead
interface SelfReviewRepository : BaseRepository<SelfReview> {
    @PreAuthorizeForUser
    @PostAuthorize(
        "hasAnyRole('HEAD', 'ADMIN') or returnObject.orElse(null)?.review?.user?.username == principal.username"
    )
    override fun findById(id: BaseEntityId): Optional<SelfReview>

    @PreAuthorize(
        "hasAnyRole('HEAD', 'ADMIN') or #entity.review.user.username == principal.username"
    )
    override fun <S : SelfReview?> save(entity: S): S
}
