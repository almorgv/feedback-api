package com.example.feedback.repositories

import com.example.feedback.models.Criteria
import com.example.feedback.models.JobRole
import com.example.feedback.models.Review
import com.example.feedback.models.Score
import com.example.feedback.models.Sheet
import com.example.feedback.models.SheetAnswer
import com.example.feedback.models.User
import com.example.feedback.security.AdminAuthToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class SheetAnswerRepositoryTest(
    @Autowired private val sheetRepository: SheetRepository,
    @Autowired private val reviewRepository: ReviewRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val jobRoleRepository: JobRoleRepository,
    @Autowired private val criteriaRepository: CriteriaRepository,
    @Autowired private val sheetAnswerRepository: SheetAnswerRepository,
    @Autowired private val mockMvc: MockMvc,
) {

    @AfterEach
    fun afterEach() {
        AdminAuthToken().setAsCurrentAuthInContext().use {
            sheetRepository.deleteAll()
            reviewRepository.deleteAll()
            userRepository.deleteAll()
            jobRoleRepository.deleteAll()
            criteriaRepository.deleteAll()
            sheetAnswerRepository.deleteAll()
        }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["USER"])
    fun `should let user update sheet answer`() {
        val sheetAnswer1: SheetAnswer

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            criteriaRepository.save(
                Criteria(
                    name = "criteria1",
                    description = "",
                    jobRole = jobRole1,
                )
            )
            val user1 = userRepository.save(
                User(
                    username = "user1",
                    jobRole = jobRole1
                )
            )
            val user2 = userRepository.save(
                User(
                    username = "user2",
                    jobRole = jobRole1,
                )
            )
            val review1 = reviewRepository.save(
                Review(
                    user = user1,
                    period = "Q1",
                )
            )
            val sheet1 = sheetRepository.save(
                Sheet(
                    review = review1,
                    reviewer = user2,
                    dueDate = LocalDate.now()
                )
            )
            sheetAnswer1 = sheetAnswerRepository.save(
                SheetAnswer(
                    sheet = sheet1,
                )
            )
            sheet1.apply {
                sheetAnswer = sheetAnswer1
                sheetRepository.save(this)
            }
        }

        mockMvc.patch("/sheetAnswers/${sheetAnswer1.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "score": "${Score.MEET_EXPECTATIONS}",
                    "comment": "some comment"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk }
        }
    }
}
