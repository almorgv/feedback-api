package com.example.feedback.controllers

import com.example.feedback.models.*
import com.example.feedback.repositories.ReviewRepository
import com.example.feedback.repositories.SheetRepository
import com.example.feedback.services.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpHeaders
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse
import kotlin.math.roundToInt

data class SheetWeight(
    val sheetId: BaseEntityId,
    val weight: Double
)

data class ReviewWeights(
    val weights: List<SheetWeight>,
)

@PostAuthorize("hasAnyRole('HEAD', 'ADMIN')")
@RestController
@RequestMapping("/reviews")
class ReviewController (
    private val reviewRepository: ReviewRepository,
    private val reviewService: ReviewService,
) {
    @Operation(
        summary = "Export review results",
        description = "Returns XLSX-file with review results"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Exported successfully"),
            ApiResponse(responseCode = "404", description = "The review wasn't found")
        ]
    )
    @GetMapping("/{reviewId}/export")
    fun getExportedReview(@PathVariable reviewId: BaseEntityId, response: HttpServletResponse) {
        val optionalReview = reviewRepository.findById(reviewId)
        if (optionalReview.isEmpty) {
            response.sendError(404, "Not found")
            return
        }

        val review = optionalReview.get()
        val filename = "${review.user.username}_${review.period}.xlsx"
        val workbook = reviewService.generateReviewReport(review)

        response.status = 200
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filename}\"")
        workbook.write(response.outputStream)
    }

    @Operation(
        summary = "Export all reviews results",
        description = "Returns XLSX-file with all reviews results"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Exported successfully")
        ]
    )
    @GetMapping("/export")
    fun getExportedAllReviews(response: HttpServletResponse) {
        val reviews = reviewRepository.findAll()
        val workbook = reviewService.generateAllReviewsReport(reviews.toList())

        response.status = 200
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reviews.xlsx\"")
        workbook.write(response.outputStream)
    }

    @Operation(
        summary = "Set weights",
        description = "Changes weights of reviewers group and reviewers"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Set successfully"),
            ApiResponse(responseCode = "412", description = "The sum of weights isn't 1.0"),
        ]
    )
    @PutMapping("/{reviewId}/weights")
    fun setReviewWeights(@RequestBody data: ReviewWeights, response: HttpServletResponse) {
        val weightSum = data.weights.map { it.weight }.sum()
        val roundedSum = (weightSum * 100).roundToInt()

        if (roundedSum != 100) {
            response.sendError(412, "Incorrect weights")
            return
        }

        reviewService.saveSheetWeights(data.weights)
    }
}
