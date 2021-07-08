package com.example.feedback.events

import com.example.feedback.exceptions.PreconditionException
import com.example.feedback.models.Sheet
import com.example.feedback.repositories.CriteriaRepository
import com.example.feedback.repositories.SheetRepository
import com.example.feedback.services.SheetService
import org.springframework.data.rest.core.annotation.HandleAfterCreate
import org.springframework.data.rest.core.annotation.HandleBeforeCreate
import org.springframework.data.rest.core.annotation.HandleBeforeSave
import org.springframework.data.rest.core.annotation.RepositoryEventHandler
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@RepositoryEventHandler
class SheetEventHandler(
    private val sheetService: SheetService,
    private val sheetRepository: SheetRepository,
    private val criteriaRepository: CriteriaRepository,
    private val eventHandlerHelper: EventHandlerHelper,
) {

    @HandleBeforeCreate
    fun handleBeforeCreate(sheet: Sheet) {
        sheet.review.user.jobRole
            ?.let {
                if (criteriaRepository.findAllByJobRole(it).isEmpty()) {
                    throw PreconditionException("Can not create sheet. Criterias for ${it.name} does not exist")
                }
            }
            ?: throw PreconditionException("Can not create sheet for reviewee without jobRole")
    }

    @HandleBeforeSave
    fun handleBeforeSave(sheet: Sheet) {
        eventHandlerHelper.withExistentEntity(sheet, sheetRepository) {
            if (it?.completed == true && sheet.completed) {
                throw AccessDeniedException("Not allowed to edit sheet marked as completed")
            }

            if (it?.completed != true && sheet.completed) {
                sheet.completedDate = Instant.now()
            }
        }
    }

    @HandleAfterCreate
    fun handleAfterCreate(sheet: Sheet) {
        sheetService.createEmptyAnswers(sheet)
        sheetService.createEmptySheetAnswer(sheet)
    }
}
