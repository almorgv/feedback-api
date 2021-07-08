package com.example.feedback.events

import com.example.feedback.models.User
import com.example.feedback.repositories.UserRepository
import com.example.feedback.security.hasRole
import org.springframework.data.rest.core.annotation.HandleBeforeSave
import org.springframework.data.rest.core.annotation.RepositoryEventHandler
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
@RepositoryEventHandler
class UserEventHandler(
    private val userRepository: UserRepository,
    private val eventHandlerHelper: EventHandlerHelper,
) {

    /**
     * Restrict changing userRole to ROLE_ADMIN only
     * Used workaround to get previous and changed entities
     * see https://github.com/spring-projects/spring-data-rest/issues/753
     */
    @HandleBeforeSave
    fun handleBeforeSave(user: User) {
        eventHandlerHelper.withExistentEntity(user, userRepository) {
            if (user.userRole != it?.userRole &&
                !SecurityContextHolder.getContext().hasRole("ADMIN")
            ) {
                throw AccessDeniedException("You are not allowed to change user role")
            }
        }
    }
}
