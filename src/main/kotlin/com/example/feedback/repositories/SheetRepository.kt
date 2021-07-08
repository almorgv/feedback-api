package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.Sheet
import org.springframework.security.access.prepost.PostAuthorize
import java.util.Optional

@PreAuthorizeForHead
interface SheetRepository : BaseRepository<Sheet> {
    @PreAuthorizeForUser
    @PostAuthorize(
        """
        hasAnyRole('HEAD', 'ADMIN')
            or returnObject.orElse(null)?.reviewer?.username == principal.username
        """
    )
    override fun findById(id: BaseEntityId): Optional<Sheet>
}
