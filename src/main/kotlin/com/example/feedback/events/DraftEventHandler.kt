package com.example.feedback.events

import com.example.feedback.exceptions.PreconditionException
import com.example.feedback.models.Draft
import org.springframework.data.rest.core.annotation.HandleBeforeCreate
import org.springframework.data.rest.core.annotation.RepositoryEventHandler
import org.springframework.stereotype.Component

@Component
@RepositoryEventHandler
class DraftEventHandler {
    @HandleBeforeCreate
    fun handleBeforeCreate(draft: Draft) {
        if (draft.user.id == draft.author.id) {
            throw PreconditionException("Can not create draft to yourself")
        }
    }
}
