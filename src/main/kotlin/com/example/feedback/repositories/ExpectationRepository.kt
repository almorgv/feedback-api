package com.example.feedback.repositories

import com.example.feedback.models.BaseEntityId
import com.example.feedback.models.Expectation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import java.util.Optional

@RepositoryRestResource
@PreAuthorizeForAdmin
interface ExpectationRepository : BaseRepository<Expectation> {
    @PreAuthorizeForUser
    override fun findById(id: BaseEntityId): Optional<Expectation>

    @PreAuthorizeForUser
    override fun findAll(sort: Sort): MutableIterable<Expectation>

    @PreAuthorizeForUser
    override fun findAll(pageable: Pageable): Page<Expectation>

    @PreAuthorizeForUser
    override fun findAll(): MutableIterable<Expectation>
}
