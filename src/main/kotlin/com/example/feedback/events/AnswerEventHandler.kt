package com.example.feedback.events

import com.example.feedback.models.Answer
import org.springframework.data.rest.core.annotation.HandleBeforeCreate
import org.springframework.data.rest.core.annotation.HandleBeforeSave
import org.springframework.data.rest.core.annotation.RepositoryEventHandler
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import javax.validation.ValidationException

@Component
@RepositoryEventHandler
class AnswerEventHandler {

    @HandleBeforeSave
    @HandleBeforeCreate
    fun handleBeforeSave(answer: Answer) {
        if (answer.sheet.completed || answer.sheet.review.completed) {
            throw AccessDeniedException("Not allowed to modify sheet marked as completed")
        }
        if (answer.sheet.review.user.jobRole?.name != answer.criteria.jobRole.name) {
            throw ValidationException("Criteria jobRole does not match with reviewee jobRole")
        }
    }
}
