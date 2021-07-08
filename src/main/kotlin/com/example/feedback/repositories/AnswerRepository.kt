package com.example.feedback.repositories

import com.example.feedback.models.Answer
import com.example.feedback.models.BaseEntityId
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import java.util.Optional

@RepositoryRestResource
@PreAuthorizeForHead
interface AnswerRepository : BaseRepository<Answer> {
    @PreAuthorizeForUser
    @PostAuthorize(
        """
        hasAnyRole('HEAD', 'ADMIN')
            or returnObject.orElse(null)?.sheet?.reviewer?.username == principal.username
            or returnObject.orElse(null)?.sheet?.review?.user?.username == principal.username
        """
    )
    override fun findById(id: BaseEntityId): Optional<Answer>

    @PreAuthorize(
        """
        hasAnyRole('HEAD', 'ADMIN')
            or #entity.sheet.reviewer.username == principal.username
            and not #entity.isNew()
        """
    )
    override fun <S : Answer?> save(entity: S): S
}
