package com.example.feedback.services

import com.example.feedback.models.Answer
import com.example.feedback.models.Sheet
import com.example.feedback.models.SheetAnswer
import com.example.feedback.repositories.AnswerRepository
import com.example.feedback.repositories.CriteriaRepository
import com.example.feedback.repositories.SheetAnswerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SheetService(
    private val answerRepository: AnswerRepository,
    private val sheetAnswerRepository: SheetAnswerRepository,
    private val criteriaRepository: CriteriaRepository,
) {
    /**
     * Create answers for all criterias of the corresponding job role of the reviewee user
     */
    fun createEmptyAnswers(sheet: Sheet) {
        sheet.review.user.jobRole
            ?.let { criteriaRepository.findAllByJobRole(it) }
            ?.takeIf { it.isNotEmpty() }
            ?.filterNot { it.archived }
            ?.map { Answer(sheet, it) }
            ?.let { answerRepository.saveAll(it) }
    }

    fun createEmptySheetAnswer(sheet: Sheet) {
        SheetAnswer(sheet)
            .apply { sheet.sheetAnswer = this }
            .let { sheetAnswerRepository.save(it) }
    }
}
