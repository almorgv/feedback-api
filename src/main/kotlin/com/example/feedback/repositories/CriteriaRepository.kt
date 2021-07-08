package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.Criteria
import com.example.feedback.models.JobRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import java.util.Optional

@RepositoryRestResource
@PreAuthorizeForAdmin
interface CriteriaRepository : BaseRepository<Criteria> {
    @PreAuthorizeForUser
    override fun findById(id: BaseEntityId): Optional<Criteria>

    @PreAuthorizeForUser
    override fun findAll(): MutableIterable<Criteria>

    @PreAuthorizeForUser
    override fun findAll(sort: Sort): MutableIterable<Criteria>

    @PreAuthorizeForUser
    override fun findAll(pageable: Pageable): Page<Criteria>

    @PreAuthorizeForUser
    fun findAllByJobRole(jobRole: JobRole): Collection<Criteria>
}
