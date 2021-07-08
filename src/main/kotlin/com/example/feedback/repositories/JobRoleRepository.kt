package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.JobRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import java.util.Optional

@RepositoryRestResource
@PreAuthorizeForAdmin
interface JobRoleRepository : BaseRepository<JobRole> {
    @PreAuthorizeForUser
    override fun findById(id: BaseEntityId): Optional<JobRole>

    @PreAuthorizeForUser
    override fun findAll(sort: Sort): MutableIterable<JobRole>

    @PreAuthorizeForUser
    override fun findAll(pageable: Pageable): Page<JobRole>

    @PreAuthorizeForUser
    override fun findAll(): MutableIterable<JobRole>
}
