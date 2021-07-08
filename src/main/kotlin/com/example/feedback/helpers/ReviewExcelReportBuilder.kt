package com.example.feedback.helpers

import com.example.feedback.models.Comment
import com.example.feedback.models.Review
import com.example.feedback.services.SheetPosition
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress

class ReviewExcelReportBuilder(val workbook: Workbook, val review: Review) {
    private val sheetPosition = SheetPosition(blockStartLineNum = 1)
    private var sheet: Sheet? = null

    private fun generateComment(comment: Comment, rowNumber: Int, columnNumber: Int = 1) {
        val commentRow = sheet?.getRow(rowNumber) ?: sheet?.createRow(rowNumber)
        commentRow?.createCell(columnNumber)?.apply {
            cellStyle = sheet.workbook.createCellStyle(
                backgroundColor = null,
                textColor = IndexedColors.BLACK,
                alignment = HorizontalAlignment.LEFT,
            )
            cellStyle.wrapText = true
        }
        commentRow?.getCell(columnNumber)?.setCellValue(comment.text)
        sheet?.addMergedRegion(CellRangeAddress(rowNumber, rowNumber, columnNumber, columnNumber + 1))
        commentRow?.heightInPoints = 75f
    }

    fun startBuilding(sheetName: String) {
        sheet = workbook.createSheet(sheetName)

        for (columnInd in 1..5) {
            sheet?.setColumnWidth(columnInd, 10000)
        }
    }

    fun generateReviewSummary() {
        val titleRow = sheet?.createRow(sheetPosition.blockStartLineNum)
        for (columnInd in 1..2) {
            titleRow?.createCell(columnInd)?.apply {
                cellStyle = sheet.workbook.createCellStyle(
                    backgroundColor = IndexedColors.LIGHT_BLUE,
                    textColor = IndexedColors.WHITE,
                    alignment = HorizontalAlignment.CENTER,
                )
            }
        }
        titleRow?.getCell(1)?.setCellValue("Общая информация о ревью")
        sheet?.addMergedRegion(CellRangeAddress(sheetPosition.blockStartLineNum, sheetPosition.blockStartLineNum, 1, 2))


        sheet?.createRow(sheetPosition.blockStartLineNum + 1)?.apply {
            createCell(1).setCellValue("Ревьюируемый:")
            createCell(2).setCellValue(review.user.fullName)
        }
        sheet?.createRow(sheetPosition.blockStartLineNum + 2)?.apply {
            createCell(1).setCellValue("Период:")
            createCell(2).setCellValue(review.period)
        }
        sheet?.createRow(sheetPosition.blockStartLineNum + 3)?.apply {
            createCell(1).setCellValue("Статус:")
            createCell(2).setCellValue(if (review.completed) "Завершено" else "В процессе")
        }
        sheet?.createRow(sheetPosition.blockStartLineNum + 4)?.apply {
            createCell(1).setCellValue("Позиция сотрудника:")
            createCell(2).setCellValue(formatPosition(review.userPosition))
        }
        sheet?.createRow(sheetPosition.blockStartLineNum + 5)?.apply {
            createCell(1).setCellValue("Результат:")
            createCell(2).setCellValue("${formatScore(review.totalResult?.score)} (${review.totalResult?.scoreValue})")
        }

        // the block of review summary takes 6 rows and 1 row for margin
        sheetPosition.blockStartLineNum += 7
    }

    fun generateCriteriaResults() {
        val criteriaResults = review.criteriaResults
        val columnCount = 5

        val criteriaResultsTitleRow = sheet?.createRow(sheetPosition.blockStartLineNum)
        for (columnInd in 1..columnCount) {
            criteriaResultsTitleRow?.createCell(columnInd)?.apply {
                cellStyle = sheet.workbook.createCellStyle(
                    backgroundColor = IndexedColors.LIGHT_BLUE,
                    textColor = IndexedColors.WHITE,
                    alignment = HorizontalAlignment.CENTER,
                )
            }
        }
        criteriaResultsTitleRow?.getCell(1)?.setCellValue("Результаты по критериям")
        sheet?.addMergedRegion(CellRangeAddress(sheetPosition.blockStartLineNum, sheetPosition.blockStartLineNum, 1, 4))

        val tableTitleRow = sheet?.createRow(sheetPosition.blockStartLineNum + 1)
        for (columnInd in 1..columnCount) {
            tableTitleRow?.createCell(columnInd)?.apply {
                cellStyle = sheet.workbook.createCellStyle(
                    backgroundColor = IndexedColors.PALE_BLUE,
                    textColor = IndexedColors.BLACK,
                    alignment = HorizontalAlignment.CENTER,
                )
            }
        }

        tableTitleRow?.apply {
            getCell(1).setCellValue("Критерий")
            getCell(2).setCellValue("Результат")
            getCell(3).setCellValue("Мин. оценка")
            getCell(4).setCellValue("Макс. оценка")
            getCell(5).setCellValue("Среднее значение")
        }

        for (i in criteriaResults.indices) {
            val criteriaResult = criteriaResults[i]

            sheet?.createRow(sheetPosition.blockStartLineNum + 2 + i)?.apply {
                createCell(1).setCellValue(criteriaResult.criteria.name)
                createCell(2).setCellValue(formatScore(criteriaResult.score).capitalize())
                createCell(3).setCellValue(formatScore(criteriaResult.minScore).capitalize())
                createCell(4).setCellValue(formatScore(criteriaResult.maxScore).capitalize())
                createCell(5).setCellValue(criteriaResult.scoreValue)
            }
        }

        // the block of criteria results takes rows amount which equals to criteria count + 2 and 1 row for margin
        sheetPosition.blockStartLineNum += criteriaResults.size + 3
    }

    fun generateTotalComments() {
        val comments = review.totalResult?.comments ?: listOf()
        val totalCommentsTitleRow = sheet?.createRow(sheetPosition.blockStartLineNum)
        for (columnInd in 1..2) {
            totalCommentsTitleRow?.createCell(columnInd)?.apply {
                cellStyle = sheet.workbook.createCellStyle(
                    backgroundColor = IndexedColors.LIGHT_BLUE,
                    textColor = IndexedColors.WHITE,
                    alignment = HorizontalAlignment.CENTER,
                )
            }
        }
        totalCommentsTitleRow?.getCell(1)?.setCellValue("Общие комментарии к анкете")
        sheet?.addMergedRegion(CellRangeAddress(sheetPosition.blockStartLineNum, sheetPosition.blockStartLineNum, 1, 2))

        for (i in comments.indices) {
            val totalComment = comments[i]
            generateComment(comment = totalComment, rowNumber = sheetPosition.blockStartLineNum + i + 1)
        }

        // the block of total comments takes rows amount which equals to comments count + 1 and 1 row for margin
        sheetPosition.blockStartLineNum += comments.size + 2
    }

    fun generateCriteriaComments() {
        val criteriaResults = review.criteriaResults
        val criteriaCommentsTitleRow = sheet?.createRow(sheetPosition.blockStartLineNum)
        for (columnInd in 1..3) {
            criteriaCommentsTitleRow?.createCell(columnInd)?.apply {
                cellStyle = sheet.workbook.createCellStyle(
                    backgroundColor = IndexedColors.LIGHT_BLUE,
                    textColor = IndexedColors.WHITE,
                    alignment = HorizontalAlignment.CENTER,
                )
            }
        }
        criteriaCommentsTitleRow?.getCell(1)?.setCellValue("Комментарии по критериям")
        sheet?.addMergedRegion(CellRangeAddress(sheetPosition.blockStartLineNum, sheetPosition.blockStartLineNum, 1, 3))

        var commentsCount = 0

        for (i in criteriaResults.indices) {
            val criteriaResult = criteriaResults[i]
            val criteriaNameLineNumber = sheetPosition.blockStartLineNum + commentsCount + 1
            val criteriaNameRow = sheet?.createRow(criteriaNameLineNumber)
            criteriaNameRow?.createCell(1)?.apply {
                cellStyle = sheet.workbook.createCellStyle(
                    backgroundColor = null,
                    textColor = IndexedColors.BLACK,
                    alignment = HorizontalAlignment.CENTER,
                    verticalAlignment = VerticalAlignment.CENTER
                )
                cellStyle.wrapText = true
                setCellValue(criteriaResult.criteria.name)
            }
            if (criteriaResult.comments.size > 1) {
                sheet?.addMergedRegion(
                    CellRangeAddress(
                        criteriaNameLineNumber, criteriaNameLineNumber + criteriaResult.comments.size - 1, 1, 1
                    )
                )
            }

            for (j in criteriaResult.comments.indices) {
                val totalComment = criteriaResult.comments[j]
                generateComment(comment = totalComment, rowNumber = criteriaNameLineNumber + j, columnNumber = 2)
            }
            commentsCount += criteriaResult.comments.size
        }

        // the block of criteria results takes rows amount which equals to comments count + 1 and 1 row for margin
        sheetPosition.blockStartLineNum += commentsCount + 2
    }

    fun build() {
        sheetPosition.blockStartLineNum = 1
    }
}
