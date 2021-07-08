@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.example.feedback.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.vladmihalcea.hibernate.type.array.EnumArrayType
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.AbstractPersistable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass

typealias BaseEntityId = java.lang.Long

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@TypeDefs(
    TypeDef(
        name = "jsonb",
        typeClass = EnumArrayType::class
    )
)
abstract class BaseEntity : AbstractPersistable<BaseEntityId>() {
    @CreatedDate
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var createdDate: Instant = Instant.EPOCH

    @LastModifiedDate
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    open var lastModifiedDate: Instant = Instant.EPOCH

    override fun toString(): String {
        return "id=$id, createdDate=$createdDate, lastModifiedDate=$lastModifiedDate"
    }
}

interface BaseEntityProjection {
    fun getCreatedDate(): Instant
    fun getLastModifiedDate(): Instant
    fun getId(): BaseEntityId
}
