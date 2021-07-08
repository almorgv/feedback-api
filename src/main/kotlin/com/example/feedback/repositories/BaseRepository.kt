package com.example.feedback.repositories

import com.example.feedback.models.BaseEntity
import com.example.feedback.models.BaseEntityId
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.PagingAndSortingRepository

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : PagingAndSortingRepository<T, BaseEntityId>
