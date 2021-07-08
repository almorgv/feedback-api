package com.example.feedback.events

import com.example.feedback.exceptions.PreconditionException
import com.example.feedback.models.Position
import com.example.feedback.models.Review
import com.example.feedback.models.SelfReview
import com.example.feedback.repositories.ReviewRepository
import com.example.feedback.repositories.SelfReviewRepository
import com.example.feedback.repositories.SheetRepository
import org.springframework.data.rest.core.annotation.HandleAfterCreate
import org.springframework.data.rest.core.annotation.HandleAfterSave
import org.springframework.data.rest.core.annotation.HandleBeforeCreate
import org.springframework.data.rest.core.annotation.HandleBeforeSave
import org.springframework.data.rest.core.annotation.RepositoryEventHandler
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@RepositoryEventHandler
class ReviewEventHandler(
    private val reviewRepository: ReviewRepository,
    private val sheetRepository: SheetRepository,
    private val selfReviewRepository: SelfReviewRepository,
    private val eventHandlerHelper: EventHandlerHelper,
) {

    @HandleBeforeCreate
    fun handleBeforeCreate(review: Review) {
        review.user.jobRole
            ?: throw PreconditionException("Can not create review for user without jobRole")

        if (review.user.position == Position.NONE) {
            throw PreconditionException("Can not create review for user without position")
        }

        review.userPosition = review.user.position
    }

    @HandleAfterCreate
    fun handleAfterCreate(review: Review) {
        selfReviewRepository.save(
            SelfReview(
                review = review,
            )
        )
    }

    @HandleBeforeSave
    fun handleBeforeSave(review: Review) {
        eventHandlerHelper.withExistentEntity(review, reviewRepository) { existentReview ->
            if (existentReview?.completed != true && review.completed) {
                review.completedDate = Instant.now()
            }
        }
    }

    @HandleAfterSave
    fun handleAfterSave(review: Review) {
        if (review.completed) {
            review.sheets
                .filterNot { it.completed }
                .forEach {
                    it.completed = true
                    it.completedDate = Instant.now()
                }
            sheetRepository.saveAll(review.sheets)
        }
    }
}
