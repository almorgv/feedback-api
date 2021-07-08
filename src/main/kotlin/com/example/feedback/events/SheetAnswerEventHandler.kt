package com.example.feedback.events

import com.example.feedback.models.SheetAnswer
import com.example.feedback.repositories.SheetRepository
import org.springframework.data.rest.core.annotation.HandleAfterCreate
import org.springframework.data.rest.core.annotation.HandleBeforeCreate
import org.springframework.data.rest.core.annotation.HandleBeforeSave
import org.springframework.data.rest.core.annotation.RepositoryEventHandler
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

@Component
@RepositoryEventHandler
class SheetAnswerEventHandler(
    private val sheetRepository: SheetRepository,
) {

    @HandleBeforeSave
    @HandleBeforeCreate
    fun handleBeforeSave(sheetAnswer: SheetAnswer) {
        if (sheetAnswer.sheet.completed || sheetAnswer.sheet.review.completed) {
            throw AccessDeniedException("Not allowed to modify sheet marked as completed")
        }
    }

    @HandleAfterCreate
    fun handleAfterCreate(sheetAnswer: SheetAnswer) {
        sheetAnswer.sheet.id
            ?.let { sheetRepository.findById(it) }
            ?.orElse(null)
            ?.apply { this.sheetAnswer = sheetAnswer }
            ?.let { sheetRepository.save(it) }
    }
}
