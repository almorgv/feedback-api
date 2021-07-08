package com.example.feedback.services

import com.example.feedback.controllers.SheetWeight
import com.example.feedback.helpers.ReviewExcelReportBuilder
import com.example.feedback.helpers.ReviewsExcelReportBuilder
import com.example.feedback.models.Review
import com.example.feedback.repositories.SheetRepository
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class SheetPosition(
    var blockStartLineNum: Int
)

@Service
class ReviewService(
    private val sheetRepository: SheetRepository,
) {
    @Transactional
    fun saveSheetWeights(weights: List<SheetWeight>) {
        weights.forEach {
            sheetRepository.findById(it.sheetId).get().apply { weight = it.weight }
        }
    }

    fun generateReviewReport(review: Review): Workbook {
        val wb: Workbook = XSSFWorkbook()
        val generator = ReviewExcelReportBuilder(workbook = wb, review = review)

        generator.apply {
            startBuilding("Ревью")
            generateReviewSummary()
            generateCriteriaResults()
            generateTotalComments()
            generateCriteriaComments()
            build()
        }

        return wb
    }

    fun generateAllReviewsReport(reviews: List<Review>): Workbook {
        val wb: Workbook = XSSFWorkbook()
        val generator = ReviewsExcelReportBuilder(workbook = wb, reviews = reviews)

        generator.apply {
            startBuilding("Общие результаты")
            generateReviewsResult()

            startBuilding("Результаты по критериям")
            generateReviewsByCriteriaResult()

            build()
        }

        return wb
    }
}
