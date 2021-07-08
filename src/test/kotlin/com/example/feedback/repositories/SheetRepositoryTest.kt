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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class SheetRepositoryTest(
    @Autowired private val sheetRepository: SheetRepository,
    @Autowired private val reviewRepository: ReviewRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val jobRoleRepository: JobRoleRepository,
    @Autowired private val criteriaRepository: CriteriaRepository,
    @Autowired private val answerRepository: AnswerRepository,
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
            answerRepository.deleteAll()
            sheetAnswerRepository.deleteAll()
        }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["USER"])
    fun `should set isFilled for sheet only after all answers is filled`() {
        val sheet1: Sheet

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            criteriaRepository.saveAll(
                listOf(
                    Criteria(
                        name = "criteria1",
                        description = "",
                        jobRole = jobRole1,
                    ),
                    Criteria(
                        name = "criteria2",
                        description = "",
                        jobRole = jobRole1,
                    )
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
            sheet1 = sheetRepository.save(
                Sheet(
                    review = review1,
                    reviewer = user2,
                    dueDate = LocalDate.now()
                )
            )
            val sheetAnswer1 = sheetAnswerRepository.save(
                SheetAnswer(
                    sheet = sheet1,
                )
            )
            sheet1.apply {
                sheetAnswer = sheetAnswer1
                sheetRepository.save(this)
            }
        }

        mockMvc.get("/sheets/${sheet1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.filled") {
                    value(false)
                }
            }

        AdminAuthToken().setAsCurrentAuthInContext().use {
            sheet1.answers.forEach {
                it.comment = "comment"
                it.score = Score.MEET_EXPECTATIONS
                answerRepository.save(it)
            }
        }

        mockMvc.get("/sheets/${sheet1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.filled") {
                    value(false)
                }
            }

        AdminAuthToken().setAsCurrentAuthInContext().use {
            sheet1.sheetAnswer!!.apply {
                comment = "comment"
                totalScore = Score.MEET_EXPECTATIONS
                sheetAnswerRepository.save(this)
            }
        }

        mockMvc.get("/sheets/${sheet1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.filled") {
                    value(true)
                }
            }
    }
}
