package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.SheetAnswer
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import java.util.Optional

@PreAuthorizeForHead
interface SheetAnswerRepository : BaseRepository<SheetAnswer> {
    @PreAuthorizeForUser
    @PostAuthorize(
        """
        hasAnyRole('HEAD', 'ADMIN')
        or returnObject.orElse(null)?.sheet?.reviewer?.username == principal.username
        """
    )
    override fun findById(id: BaseEntityId): Optional<SheetAnswer>

    @PreAuthorize(
        """
        hasAnyRole('HEAD', 'ADMIN')
        or #entity.sheet.reviewer.username == principal.username
        """
    )
    override fun <S : SheetAnswer?> save(entity: S): S
}
