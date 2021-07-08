package com.example.feedback.events

import com.example.feedback.models.BaseEntity
import com.example.feedback.repositories.BaseRepository
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

/**
 * Workaround to get previous and changed entities in @RepositoryEventHandler
 * see https://github.com/spring-projects/spring-data-rest/issues/753
 */
@Component
class EventHandlerHelper {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    fun <T : BaseEntity, R> withExistentEntity(entity: T, repo: BaseRepository<T>, block: (T?) -> R): R {
        return entity.withDetachedContext {
            val existentEntity = entity.id
                ?.let { repo.findById(it) }
                ?.orElse(null)
            block(existentEntity)
        }
    }

    private fun <T : BaseEntity, R> T.withDetachedContext(block: () -> R): R {
        return try {
            entityManager.detach(this)
            block()
        } finally {
            entityManager.runCatching { merge(this@withDetachedContext) }
        }
    }
}
