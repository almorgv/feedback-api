package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.Draft
import org.springframework.data.jpa.repository.Query
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import java.util.Optional

@PreAuthorizeForAdmin
@RepositoryRestResource
interface DraftRepository : BaseRepository<Draft> {
    @PreAuthorizeForUser
    @PostAuthorize(
        """
        returnObject.orElse(null)?.author?.username == principal.username
        """
    )
    override fun findById(id: BaseEntityId): Optional<Draft>

    @PreAuthorizeForUser
    @Query(
        """
        select d
        from #{#entityName} d
        where d.author.id = ?#{principal.id} and d.user.id=:id
    """
    )
    fun getCurrentUserDraftByUserId(id: BaseEntityId): Draft?


    @PreAuthorize(
        """
        hasAnyRole('USER', 'HEAD', 'ADMIN')
            and #entity.author.username == principal.username
        """
    )
    override fun <S : Draft> save(entity: S): S

    @PreAuthorize(
        """
        #entity.author.username == principal.username
        """
    )
    override fun delete(entity: Draft)
}
