package com.example.feedback.helpers

import com.example.feedback.models.Review
import com.example.feedback.services.SheetPosition
import org.apache.poi.ss.usermodel.*

class ReviewsExcelReportBuilder(val workbook: Workbook, val reviews: List<Review>) {
    private val sheetPosition = SheetPosition(blockStartLineNum = 1)
    private var activeSheet: Sheet? = null

    private fun setupColumns(criteria: Boolean) {
        val columnsWidth =
            if (criteria)
                listOf(10000, 5000, 5000, 5000, 4000, 10000, 9000, 3500, 3500, 3500)
            else
                listOf(10000, 5000, 5000, 5000, 4000, 9000, 3500)

        for (i in columnsWidth.indices) {
            activeSheet?.setColumnWidth(i, columnsWidth[i])
        }

        val columnsTitles =
            if (criteria)
                listOf("Пользователь", "Ревью", "Позиция", "Дата завершения", "Завершено", "Критерий", "Результат", "Значение", "Мин.", "Макс.")
            else
                listOf("Пользователь", "Ревью", "Позиция", "Дата завершения", "Завершено", "Результат", "Значение")

        activeSheet?.createRow(sheetPosition.blockStartLineNum)?.apply {
            for (i in columnsTitles.indices) {
                createCell(i).apply {
                    setCellValue(columnsTitles[i])
                    cellStyle = sheet.workbook.createCellStyle(
                        backgroundColor = IndexedColors.PALE_BLUE,
                        textColor = IndexedColors.BLACK,
                        alignment = HorizontalAlignment.CENTER,
                    )
                }
            }
        }
    }

    fun startBuilding(sheetName: String) {
        activeSheet = workbook.createSheet(sheetName)
        sheetPosition.blockStartLineNum = 0
    }

    fun generateReviewsResult() {
        setupColumns(criteria = false)

        for (i in reviews.indices) {
            val review = reviews[i]

            activeSheet?.createRow(sheetPosition.blockStartLineNum + i + 1)?.apply {
                createCell(0).setCellValue(review.user.fullName)
                createCell(1).setCellValue(review.period)
                createCell(2).setCellValue(formatPosition(review.userPosition))
                createCell(3).setCellValue(review.completedDate?.toString()?.split("T")?.get(0) ?: "")
                createCell(4).setCellValue(if (review.completed) "да" else "нет")
                createCell(5).setCellValue(formatScore(review.totalResult?.score))
                createCell(6).setCellValue(review.totalResult?.scoreValue ?: 0.0)
            }
        }

        sheetPosition.blockStartLineNum += reviews.size + 2
    }

    fun generateReviewsByCriteriaResult() {
        setupColumns(criteria = true)

        for (review in reviews) {
            val criteriaCnt = review.criteriaResults.size

            for (j in review.criteriaResults.indices) {
                val criteriaResult = review.criteriaResults[j]
                val criteriaResultRowNum = sheetPosition.blockStartLineNum + j + 1
                val criteriaResultRow = activeSheet?.getRow(criteriaResultRowNum)
                    ?: activeSheet?.createRow(criteriaResultRowNum)

                criteriaResultRow?.apply {
                    createCell(0).setCellValue(review.user.fullName)
                    createCell(1).setCellValue(review.period)
                    createCell(2).setCellValue(formatPosition(review.userPosition))
                    createCell(3).setCellValue(review.completedDate?.toString()?.split("T")?.get(0) ?: "")
                    createCell(4).setCellValue(if (review.completed) "да" else "нет")
                    createCell(5).setCellValue(criteriaResult.criteria.name)
                    createCell(6).setCellValue(formatScore(criteriaResult.score))
                    createCell(7).setCellValue(criteriaResult.scoreValue)
                    createCell(8).setCellValue(criteriaResult.minScoreValue)
                    createCell(9).setCellValue(criteriaResult.maxScoreValue)
                }
            }

            sheetPosition.blockStartLineNum += criteriaCnt
        }
    }

    fun build() {
        sheetPosition.blockStartLineNum = 0
        activeSheet = null
    }
}
