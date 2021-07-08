package com.example.feedback.repositories

import com.example.feedback.models.Answer
import com.example.feedback.models.Criteria
import com.example.feedback.models.JobRole
import com.example.feedback.models.Review
import com.example.feedback.models.Score
import com.example.feedback.models.Sheet
import com.example.feedback.models.SheetAnswer
import com.example.feedback.models.User
import com.example.feedback.security.AdminAuthToken
import org.hamcrest.CoreMatchers
import org.hamcrest.core.Every
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
internal class ReviewRepositoryTest(
    @Autowired private val sheetRepository: SheetRepository,
    @Autowired private val reviewRepository: ReviewRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val jobRoleRepository: JobRoleRepository,
    @Autowired private val criteriaRepository: CriteriaRepository,
    @Autowired private val answerRepository: AnswerRepository,
    @Autowired private val sheetAnswerRepository: SheetAnswerRepository,
    @Autowired private val mockMvc: MockMvc,
) {
    private fun constructTestReview(
        withAnswers: Boolean,
        withSheetAnswers: Boolean,
        withMissedComments: Boolean
    ): Review {
        val review: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(JobRole(name = "jobRole1"))
            val criteria1 = criteriaRepository.save(Criteria(name = "criteria1", description = "", jobRole = jobRole1))
            val criteria2 = criteriaRepository.save(Criteria(name = "criteria2", description = "", jobRole = jobRole1))
            val user1 = userRepository.save(User(username = "user1", jobRole = jobRole1))
            val user2 = userRepository.save(User(username = "user2", jobRole = jobRole1))
            val user3 = userRepository.save(User(username = "user3", jobRole = jobRole1))
            val user4 = userRepository.save(User(username = "user4", jobRole = jobRole1))
            review = reviewRepository.save(Review(user = user1, period = "Q1"))
            val sheet1 = sheetRepository.save(Sheet(review = review, reviewer = user2, dueDate = LocalDate.now(), weight = 0.5))
            val sheet2 = sheetRepository.save(Sheet(review = review, reviewer = user3, dueDate = LocalDate.now(), weight = 0.3))
            val sheet3 = sheetRepository.save(Sheet(review = review, reviewer = user4, dueDate = LocalDate.now(), weight = 0.2))
            if (withAnswers) {
                answerRepository.saveAll(
                    listOf(
                        // test scores 1, 5 for first criteria
                        Answer(
                            sheet = sheet1,
                            criteria = criteria1,
                            score = Score.NONE,
                            comment = "1-1"
                        ),
                        Answer(
                            sheet = sheet2,
                            criteria = criteria1,
                            score = Score.WAY_BELOW_EXPECTATIONS,
                            comment = if (!withMissedComments) "2-1" else null
                        ),
                        Answer(
                            sheet = sheet3,
                            criteria = criteria1,
                            score = Score.WAY_ABOVE_EXPECTATIONS,
                            comment = "3-1"
                        ),

                        // test scores 2, 3 for second criteria
                        Answer(
                            sheet = sheet1,
                            criteria = criteria2,
                            score = Score.MEET_EXPECTATIONS,
                            comment = if (!withMissedComments) "1-2" else null
                        ),
                        Answer(
                            sheet = sheet2,
                            criteria = criteria2,
                            score = Score.BELOW_EXPECTATIONS,
                            comment = "2-2"
                        ),
                    )
                )
            }
            if (withSheetAnswers) {
                val sheetAnswer1 = sheetAnswerRepository.save(
                    SheetAnswer(
                        sheet = sheet1,
                        comment = "1",
                        totalScore = Score.MEET_EXPECTATIONS
                    )
                )
                val sheetAnswer2 = sheetAnswerRepository.save(
                    SheetAnswer(
                        sheet = sheet2,
                        comment = if (!withMissedComments) "2" else null,
                        totalScore = Score.BELOW_EXPECTATIONS
                    )
                )
                val sheetAnswer3 = sheetAnswerRepository.save(
                    SheetAnswer(
                        sheet = sheet3,
                        comment = "3",
                        totalScore = Score.NONE
                    )
                )
                sheet1.apply {
                    sheetAnswer = sheetAnswer1
                    sheetRepository.save(this)
                }
                sheet2.apply {
                    sheetAnswer = sheetAnswer2
                    sheetRepository.save(this)
                }
                sheet3.apply {
                    sheetAnswer = sheetAnswer3
                    sheetRepository.save(this)
                }
            }
        }

        return review
    }

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
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should correct count criteria results`() {
        val review = constructTestReview(withAnswers = true, withSheetAnswers = false, withMissedComments = false)

        /*
        * In sheet1: weight = 0.5; answers = 0, 3
        * In sheet2: weight = 0.3; answers = 1, 2
        * In sheet3: weight = 0.2; answers = 5, 0
        *
        * total result should be:
        *   for criteria 1: (0.3 * 1 + 0.2 * 5) / 0.5 = 2.6
        *   for criteria 2: (0.5 * 3 + 0.3 * 2) / 0.8 = 2.625 = 2.63
        * */

        mockMvc.get("/reviews/${review.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.criteriaResults") {
                    exists()
                    isArray
                }
                jsonPath("$.criteriaResults.length()") {
                    value(2)
                }
                jsonPath("$.criteriaResults[0].comments.length()") {
                    value(3)
                }
                jsonPath("$.criteriaResults[1].comments.length()") {
                    value(2)
                }
                jsonPath("$.criteriaResults[0].comments[0].text") {
                    value("1-1")
                }
                jsonPath("$.criteriaResults[0].comments[1].text") {
                    value("2-1")
                }
                jsonPath("$.criteriaResults[0].comments[2].text") {
                    value("3-1")
                }
                jsonPath("$.criteriaResults[1].comments[0].text") {
                    value("1-2")
                }
                jsonPath("$.criteriaResults[1].comments[1].text") {
                    value("2-2")
                }
                jsonPath("$.criteriaResults[0].score") {
                    value(Score.MEET_EXPECTATIONS.toString())
                }
                jsonPath("$.criteriaResults[0].scoreValue") {
                    value(2.6)
                }
                jsonPath("$.criteriaResults[0].minScore") {
                    value(Score.WAY_BELOW_EXPECTATIONS.toString())
                }
                jsonPath("$.criteriaResults[0].minScoreValue") {
                    value(1)
                }
                jsonPath("$.criteriaResults[0].maxScore") {
                    value(Score.WAY_ABOVE_EXPECTATIONS.toString())
                }
                jsonPath("$.criteriaResults[0].maxScoreValue") {
                    value(5)
                }
                jsonPath("$.criteriaResults[1].score") {
                    value(Score.MEET_EXPECTATIONS.toString())
                }
                jsonPath("$.criteriaResults[1].scoreValue") {
                    value(2.63)
                }
                jsonPath("$.criteriaResults[1].minScore") {
                    value(Score.BELOW_EXPECTATIONS.toString())
                }
                jsonPath("$.criteriaResults[1].minScoreValue") {
                    value(2)
                }
                jsonPath("$.criteriaResults[1].maxScore") {
                    value(Score.MEET_EXPECTATIONS.toString())
                }
                jsonPath("$.criteriaResults[1].maxScoreValue") {
                    value(3)
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should correct count total result`() {
        val review = constructTestReview(withAnswers = true, withSheetAnswers = true, withMissedComments = false)

        /*
        * In sheet1: weight = 0.5; answers = 0, 3; sheetAnswer = 3 (totalScoreValue = 3)
        * In sheet2: weight = 0.3; answers = 1, 2; sheetAnswer = 2 (totalScoreValue = 1.667)
        * In sheet3: weight = 0.2; answers = 5, 0; sheetAnswer = 0 (totalScoreValue = 5)
        *
        * total result should be 3 * 0.5 + 1.667 * 0.3 + 5 * 0.2 = 3.0001 = 3
        * */

        mockMvc.get("/reviews/${review.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.totalResult") {
                    exists()
                }
                jsonPath("$.totalResult.score") {
                    value(Score.MEET_EXPECTATIONS.toString())
                }
                jsonPath("$.totalResult.scoreValue") {
                    value(3)
                }
                jsonPath("$.totalResult.comments.length()") {
                    value(3)
                }
                jsonPath("$.totalResult.comments[0].text") {
                    value("1")
                }
                jsonPath("$.totalResult.comments[1].text") {
                    value("2")
                }
                jsonPath("$.totalResult.comments[2].text") {
                    value("3")
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should correct count criteria results for review without sheets`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.criteriaResults") {
                    exists()
                    isArray
                }
                jsonPath("$.criteriaResults.length()") {
                    value(0)
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should correct count total result for review without sheets`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.totalResult") {
                    exists()
                }
                jsonPath("$.totalResult.comments.length()") {
                    value(0)
                }
                jsonPath("$.totalResult.scoreValue") {
                    value(0.0)
                }
                jsonPath("$.totalResult.score") {
                    value(Score.NONE.toString())
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should correct count criteria results for review without answers`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            val user3 = userRepository.save(
                User(username = "user3", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            sheetRepository.saveAll(
                listOf(
                    Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now()),
                    Sheet(review = review1, reviewer = user3, dueDate = LocalDate.now())
                )
            )
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.criteriaResults") {
                    exists()
                    isArray
                }
                jsonPath("$.criteriaResults.length()") {
                    value(0)
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should correct count total result for review without answers`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            val user3 = userRepository.save(
                User(username = "user3", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            sheetRepository.saveAll(
                listOf(
                    Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now()),
                    Sheet(review = review1, reviewer = user3, dueDate = LocalDate.now())
                )
            )
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.totalResult") {
                    exists()
                }
                jsonPath("$.totalResult.comments.length()") {
                    value(0)
                }
                jsonPath("$.totalResult.scoreValue") {
                    value(0.0)
                }
                jsonPath("$.totalResult.score") {
                    value(Score.NONE.toString())
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should correct count criteria results with missed comments`() {
        val review = constructTestReview(withAnswers = true, withSheetAnswers = false, withMissedComments = true)

        /*
        * In sheet1: weight = 0.5; answers = 0, 3
        * In sheet2: weight = 0.3; answers = 1, 2
        * In sheet3: weight = 0.2; answers = 5, 0
        *
        * total result should be:
        *   for criteria 1: (0.3 * 1 + 0.2 * 5) / 0.5 = 2.6
        *   for criteria 2: (0.5 * 3 + 0.3 * 2) / 0.8 = 2.625 = 2.63
        * */

        mockMvc.get("/reviews/${review.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.criteriaResults") {
                    exists()
                    isArray
                }
                jsonPath("$.criteriaResults.length()") {
                    value(2)
                }
                jsonPath("$.criteriaResults[0].comments.length()") {
                    value(2)
                }
                jsonPath("$.criteriaResults[1].comments.length()") {
                    value(1)
                }
                jsonPath("$.criteriaResults[0].comments[0].text") {
                    value("1-1")
                }
                jsonPath("$.criteriaResults[0].comments[1].text") {
                    value("3-1")
                }
                jsonPath("$.criteriaResults[1].comments[0].text") {
                    value("2-2")
                }
                jsonPath("$.criteriaResults[0].score") {
                    value(Score.MEET_EXPECTATIONS.toString())
                }
                jsonPath("$.criteriaResults[0].scoreValue") {
                    value(2.6)
                }
                jsonPath("$.criteriaResults[1].score") {
                    value(Score.MEET_EXPECTATIONS.toString())
                }
                jsonPath("$.criteriaResults[1].scoreValue") {
                    value(2.63)
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should correct count total result with missed comments`() {
        val review = constructTestReview(withAnswers = false, withSheetAnswers = true, withMissedComments = true)

        /*
        * In sheet1: weight = 0.5; sheetAnswer = 3 (totalScoreValue = 3)
        * In sheet2: weight = 0.3; sheetAnswer = 2 (totalScoreValue = 2)
        * In sheet3: weight = 0.2; sheetAnswer = 0 (totalScoreValue = 0)
        *
        * total result should be (3 * 0.5 + 2 * 0.3) / 0.8 = 2.625 = 2.63
        * */

        mockMvc.get("/reviews/${review.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.totalResult") {
                    exists()
                }
                jsonPath("$.totalResult.score") {
                    value(Score.MEET_EXPECTATIONS.toString())
                }
                jsonPath("$.totalResult.scoreValue") {
                    value(2.63)
                }
                jsonPath("$.totalResult.comments.length()") {
                    value(2)
                }
                jsonPath("$.totalResult.comments[0].text") {
                    value("1")
                }
                jsonPath("$.totalResult.comments[1].text") {
                    value("3")
                }
            }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @WithMockUser(username = "user1", roles = ["USER"])
    fun `should not show criteria results on not competed review for user`(completed: Boolean) {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val criteria1 = criteriaRepository.save(
                Criteria(name = "criteria1", description = "", jobRole = jobRole1)
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            val sheet1 = sheetRepository.save(
                Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now())
            )
            review1.apply {
                this.completed = completed
                reviewRepository.save(this)
            }
            answerRepository.saveAll(
                listOf(
                    Answer(sheet = sheet1, criteria = criteria1, score = Score.MEET_EXPECTATIONS, comment = "1")
                )
            )
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.criteriaResults") {
                    if (completed) {
                        exists()
                    } else {
                        doesNotExist()
                    }
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should always show criteria results for head`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val criteria1 = criteriaRepository.save(
                Criteria(name = "criteria1", description = "", jobRole = jobRole1)
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            val sheet1 = sheetRepository.save(
                Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now())
            )
            answerRepository.saveAll(
                listOf(
                    Answer(sheet = sheet1, criteria = criteria1, score = Score.MEET_EXPECTATIONS, comment = "1")
                )
            )
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.criteriaResults") {
                    exists()
                }
            }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @WithMockUser(username = "user1", roles = ["USER"])
    fun `should not show total result on not competed review for user`(completed: Boolean) {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            val sheet1 = sheetRepository.save(
                Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now())
            )
            val sheetAnswer1 = sheetAnswerRepository.save(
                SheetAnswer(sheet = sheet1, comment = "1", totalScore = Score.MEET_EXPECTATIONS)
            )

            sheet1.apply {
                sheetAnswer = sheetAnswer1
                sheetRepository.save(this)
            }

            review1.apply {
                this.completed = completed
                reviewRepository.save(this)
            }
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.totalResult") {
                    if (completed) {
                        exists()
                    } else {
                        doesNotExist()
                    }
                }
            }
    }

    @Test
    @WithMockUser(username = "user2", roles = ["HEAD"])
    fun `should always show total result for head`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            val sheet1 = sheetRepository.save(
                Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now())
            )
            val sheetAnswer1 = sheetAnswerRepository.save(
                SheetAnswer(sheet = sheet1, comment = "1", totalScore = Score.MEET_EXPECTATIONS)
            )
            sheet1.apply {
                sheetAnswer = sheetAnswer1
                sheetRepository.save(this)
            }
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.totalResult") {
                    exists()
                }
            }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @WithMockUser(username = "user1", roles = ["HEAD"])
    fun `should find all completed and uncompleted review`(completed: Boolean) {
        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            val user3 = userRepository.save(
                User(username = "user3", jobRole = jobRole1)
            )
            reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            reviewRepository.save(
                Review(user = user2, period = "Q1", completed = true)
            )
            reviewRepository.save(
                Review(user = user3, period = "Q1")
            )
        }

        val apiMethod = if (completed) "findAllByCompletedTrue" else "findAllByCompletedFalse"
        val reviewsCount = if (completed) 1 else 2

        mockMvc.get("/reviews/search/${apiMethod}")
            .andExpect {
                status { isOk }
                jsonPath("$._embedded.reviews.length()") {
                    value(reviewsCount)
                }
                jsonPath(
                    "$._embedded.reviews[*].completed",
                    Every.everyItem(CoreMatchers.equalTo(completed))
                )
            }
    }

    @Test
    @WithMockUser(username = "user1", roles = ["USER"])
    fun `should not show sheetCounters for users`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            sheetRepository.save(
                Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now())
            )
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.sheetCounters") {
                    doesNotExist()
                }
            }
    }

    @Test
    @WithMockUser(username = "user1", roles = ["HEAD"])
    fun `should show sheetCounters for head`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(name = "jobRole1")
            )
            val user1 = userRepository.save(
                User(username = "user1", jobRole = jobRole1)
            )
            val user2 = userRepository.save(
                User(username = "user2", jobRole = jobRole1)
            )
            review1 = reviewRepository.save(
                Review(user = user1, period = "Q1")
            )
            sheetRepository.save(
                Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now())
            )
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.sheetCounters") {
                    exists()
                }
                jsonPath("$.sheetCounters.all") { value(1) }
                jsonPath("$.sheetCounters.filled") { value(0) }
                jsonPath("$.sheetCounters.completed") { value(0) }
            }
    }

    @Test
    @WithMockUser(username = "user1", roles = ["HEAD"])
    fun `should count sheetCounters correct`() {
        val review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(JobRole(name = "jobRole1"))

            val criteria1 = criteriaRepository.save(Criteria(name = "criteria1", description = "", jobRole = jobRole1))
            val criteria2 = criteriaRepository.save(Criteria(name = "criteria2", description = "", jobRole = jobRole1))

            val user1 = userRepository.save(User(username = "user1", jobRole = jobRole1))
            val user2 = userRepository.save(User(username = "user2", jobRole = jobRole1))
            val user3 = userRepository.save(User(username = "user3", jobRole = jobRole1))
            val user4 = userRepository.save(User(username = "user4", jobRole = jobRole1))
            val user5 = userRepository.save(User(username = "user5", jobRole = jobRole1))

            review1 = reviewRepository.save(Review(user = user1, period = "Q1"))

            // sheet 1 is opened and empty
            sheetRepository.save(
                Sheet(review = review1, reviewer = user2, dueDate = LocalDate.now())
            )

            // sheet 2 is filled but not completed
            val sheet2 = sheetRepository.save(
                Sheet(review = review1, reviewer = user3, dueDate = LocalDate.now())
            )
            answerRepository.saveAll(listOf(
                Answer(sheet = sheet2, criteria = criteria1, score = Score.MEET_EXPECTATIONS, comment = "2-1"),
                Answer(sheet = sheet2, criteria = criteria2, score = Score.MEET_EXPECTATIONS, comment = "2-2")
            ))
            val sheetAnswer2 = sheetAnswerRepository.save(
                SheetAnswer(sheet = sheet2, comment = "2", totalScore = Score.BELOW_EXPECTATIONS)
            )
            sheet2.apply {
                sheetAnswer = sheetAnswer2
                sheetRepository.save(this)
            }

            // sheet 3 is filled but not completed too
            val sheet3 = sheetRepository.save(
                Sheet(review = review1, reviewer = user4, dueDate = LocalDate.now())
            )
            answerRepository.saveAll(listOf(
                Answer(sheet = sheet3, criteria = criteria1, score = Score.MEET_EXPECTATIONS, comment = "3-1"),
                Answer(sheet = sheet3, criteria = criteria2, score = Score.MEET_EXPECTATIONS, comment = "3-2")
            ))
            val sheetAnswer3 = sheetAnswerRepository.save(
                SheetAnswer(sheet = sheet3, comment = "3", totalScore = Score.BELOW_EXPECTATIONS)
            )
            sheet3.apply {
                sheetAnswer = sheetAnswer3
                sheetRepository.save(this)
            }

            // sheet 4 is filled and completed
            val sheet4 = sheetRepository.save(
                Sheet(review = review1, reviewer = user5, dueDate = LocalDate.now())
            )
            answerRepository.saveAll(listOf(
                Answer(sheet = sheet4, criteria = criteria1, score = Score.MEET_EXPECTATIONS, comment = "4-1"),
                Answer(sheet = sheet4, criteria = criteria2, score = Score.MEET_EXPECTATIONS, comment = "4-2")
            ))
            val sheetAnswer4 = sheetAnswerRepository.save(
                SheetAnswer(sheet = sheet4, comment = "4", totalScore = Score.BELOW_EXPECTATIONS)
            )
            sheet4.apply {
                sheetAnswer = sheetAnswer4
                completed = true
                sheetRepository.save(this)
            }
        }

        mockMvc.get("/reviews/${review1.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.sheetCounters") {
                    exists()
                }
                jsonPath("$.sheetCounters.all") { value(4) }
                jsonPath("$.sheetCounters.filled") { value(2) }
                jsonPath("$.sheetCounters.completed") { value(1) }
            }
    }

    @Test
    @WithMockUser(username = "user1", roles = ["USER"])
    fun `should not show reviewers for users`() {
        val review = constructTestReview(withAnswers = false, withSheetAnswers = true, withMissedComments = false)

        mockMvc.get("/reviews/${review.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.reviewers") {
                    doesNotExist()
                }
            }
    }

    @Test
    @WithMockUser(username = "user1", roles = ["HEAD"])
    fun `should show reviewers for head`() {
        val review = constructTestReview(withAnswers = false, withSheetAnswers = true, withMissedComments = false)

        mockMvc.get("/reviews/${review.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.reviewers") {
                    exists()
                    isArray
                }
            }
    }

    @Test
    @WithMockUser(username = "user1", roles = ["HEAD"])
    fun `should count reviewers correct`() {
        val review = constructTestReview(withAnswers = false, withSheetAnswers = true, withMissedComments = false)

        mockMvc.get("/reviews/${review.id}")
            .andExpect {
                status { isOk }
                jsonPath("$.reviewers") {
                    exists()
                    isArray
                }
                jsonPath("$.reviewers.length()") { value(3) }
                jsonPath("$.reviewers[0].username") { value("user2") }
                jsonPath("$.reviewers[1].username") { value("user3") }
                jsonPath("$.reviewers[2].username") { value("user4") }
            }
    }
}
