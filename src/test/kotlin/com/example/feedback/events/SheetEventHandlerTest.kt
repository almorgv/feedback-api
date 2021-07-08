package com.example.feedback.events

import com.example.feedback.getEntityId
import com.example.feedback.models.Answer
import com.example.feedback.models.Criteria
import com.example.feedback.models.JobRole
import com.example.feedback.models.Review
import com.example.feedback.models.ReviewerGroup
import com.example.feedback.models.Sheet
import com.example.feedback.models.User
import com.example.feedback.repositories.AnswerRepository
import com.example.feedback.repositories.CriteriaRepository
import com.example.feedback.repositories.JobRoleRepository
import com.example.feedback.repositories.ReviewRepository
import com.example.feedback.repositories.SheetRepository
import com.example.feedback.repositories.UserRepository
import com.example.feedback.security.AdminAuthToken
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class SheetEventHandlerTest(
    @Autowired private val sheetRepository: SheetRepository,
    @Autowired private val reviewRepository: ReviewRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val jobRoleRepository: JobRoleRepository,
    @Autowired private val criteriaRepository: CriteriaRepository,
    @Autowired private val answerRepository: AnswerRepository,
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
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should fail to create sheet for reviewee without jobRole`() {
        var user1: User
        var review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            user1 = userRepository.save(
                User(
                    username = "user1",
                )
            )
            review1 = reviewRepository.save(
                Review(
                    user = user1,
                    period = "Q1",
                )
            )
        }

        mockMvc.post("/sheets") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "review": "/reviews/${review1.id}",
                    "reviewer": "/users/${user1.id}",
                    "dueDate": "${LocalDate.now()}",
                    "reviewerGroup": "${ReviewerGroup.COLLEAGUE}"
                }
            """.trimIndent()
        }.andExpect {
            status { isPreconditionFailed }
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should fail to create sheet while citerias for job role does not exist`() {
        var user1: User
        var review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            user1 = userRepository.save(
                User(
                    username = "user1",
                    jobRole = jobRole1,
                )
            )
            review1 = reviewRepository.save(
                Review(
                    user = user1,
                    period = "Q1",
                )
            )
        }

        mockMvc.post("/sheets") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "review": "/reviews/${review1.id}",
                    "reviewer": "/users/${user1.id}",
                    "dueDate": "${LocalDate.now()}",
                    "reviewerGroup": "${ReviewerGroup.COLLEAGUE}"
                }
            """.trimIndent()
        }.andExpect {
            status { isPreconditionFailed }
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should create empty answers after sheet creation`() {
        var user1: User
        var review1: Review
        var criteriasJobRole1: Iterable<Criteria>

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            val jobRole2 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole2",
                )
            )
            criteriasJobRole1 = criteriaRepository.saveAll(
                listOf(
                    Criteria(
                        name = "jobRole1Criteria1",
                        description = "description1",
                        jobRole = jobRole1,
                    ),
                    Criteria(
                        name = "jobRole1Criteria2",
                        description = "description2",
                        jobRole = jobRole1,
                    ),
                    Criteria(
                        name = "jobRole1Criteria3",
                        description = "description3",
                        jobRole = jobRole1,
                    ),
                )
            )
            criteriaRepository.saveAll(
                listOf(
                    Criteria(
                        name = "jobRole2Criteria1",
                        description = "description1",
                        jobRole = jobRole2,
                    ),
                    Criteria(
                        name = "jobRole2Criteria2",
                        description = "description2",
                        jobRole = jobRole2,
                    ),
                )
            )
            user1 = userRepository.save(
                User(
                    username = "user1",
                    jobRole = jobRole1,
                )
            )
            review1 = reviewRepository.save(
                Review(
                    user = user1,
                    period = "Q1",
                )
            )
        }

        val res = mockMvc.post("/sheets") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "review": "/reviews/${review1.id}",
                    "reviewer": "/users/${user1.id}",
                    "dueDate": "${LocalDate.now()}",
                    "reviewerGroup": "${ReviewerGroup.COLLEAGUE}"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated }
        }.andReturn()

        val id = res.getEntityId()
        AdminAuthToken().setAsCurrentAuthInContext().use {
            assertThat(sheetRepository.findById(id))
                .get()
                .extracting { it.answers }
                .asInstanceOf(InstanceOfAssertFactories.list(Answer::class.java))
                .hasSameSizeAs(criteriasJobRole1)
                .extracting<Criteria> { it.criteria }
                .containsExactlyInAnyOrderElementsOf(criteriasJobRole1)
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should not create answers for archived criterias`() {
        var user1: User
        var review1: Review
        var criteriasJobRole1: Iterable<Criteria>

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            criteriasJobRole1 = criteriaRepository.saveAll(
                listOf(
                    Criteria(
                        name = "jobRole1Criteria1",
                        description = "description1",
                        jobRole = jobRole1,
                    ),
                    Criteria(
                        name = "jobRole1Criteria2",
                        description = "description2",
                        jobRole = jobRole1,
                        archived = true,
                    ),
                    Criteria(
                        name = "jobRole1Criteria3",
                        description = "description3",
                        jobRole = jobRole1,
                    ),
                )
            )
            user1 = userRepository.save(
                User(
                    username = "user1",
                    jobRole = jobRole1,
                )
            )
            review1 = reviewRepository.save(
                Review(
                    user = user1,
                    period = "Q1",
                )
            )
        }

        val res = mockMvc.post("/sheets") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "review": "/reviews/${review1.id}",
                    "reviewer": "/users/${user1.id}",
                    "dueDate": "${LocalDate.now()}",
                    "reviewerGroup": "${ReviewerGroup.COLLEAGUE}"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated }
        }.andReturn()

        AdminAuthToken().setAsCurrentAuthInContext().use {
            assertThat(sheetRepository.findById(res.getEntityId()))
                .get()
                .extracting { it.answers }
                .asInstanceOf(InstanceOfAssertFactories.list(Answer::class.java))
                .extracting<Criteria> { it.criteria }
                .containsExactlyInAnyOrderElementsOf(criteriasJobRole1.filterNot { it.archived })
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "2021-03-03T00:00:00.000000Z",
            "2021-03-03T03:00:00.000000Z",
        ]
    )
    @WithMockUser(roles = ["HEAD"])
    fun `should set completion date`(timestamp: String) {
        var sheet1: Sheet

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            criteriaRepository.save(
                Criteria(
                    name = "jobRole1Criteria1",
                    description = "description1",
                    jobRole = jobRole1,
                )
            )
            val user1 = userRepository.save(
                User(
                    username = "user1",
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
                    reviewer = user1,
                    dueDate = LocalDate.now(),
                ).apply {
                    if (timestamp.isNotEmpty()) {
                        completedDate = Instant.parse(timestamp)
                    }
                }
            )
        }

        mockMvc.patch("/sheets/${sheet1.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "completed": true
                }
            """.trimIndent()
        }.andExpect {
            status { isOk }
        }

        AdminAuthToken().setAsCurrentAuthInContext().use {
            assertThat(sheetRepository.findById(sheet1.id!!))
                .get()
                .matches {
                    it.completed &&
                        it.completedDate!!
                            .isAfter(Instant.now().minusSeconds(10)) &&
                        it.completedDate!!
                            .isBefore(Instant.now())
                }
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should not update completion date on reopening`() {
        var sheet1: Sheet

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            criteriaRepository.save(
                Criteria(
                    name = "jobRole1Criteria1",
                    description = "description1",
                    jobRole = jobRole1,
                )
            )
            val user1 = userRepository.save(
                User(
                    username = "user1",
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
                    reviewer = user1,
                    dueDate = LocalDate.now(),
                    completed = true,
                    completedDate = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                )
            )
        }

        mockMvc.patch("/sheets/${sheet1.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "completed": false
                }
            """.trimIndent()
        }.andExpect {
            status { isOk }
        }

        AdminAuthToken().setAsCurrentAuthInContext().use {
            assertThat(sheetRepository.findById(sheet1.id!!))
                .get()
                .matches {
                    !it.completed && it.completedDate == sheet1.completedDate
                }
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should fail attempt to edit completed sheet`() {
        var sheet1: Sheet

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            criteriaRepository.save(
                Criteria(
                    name = "jobRole1Criteria1",
                    description = "description1",
                    jobRole = jobRole1,
                )
            )
            val user1 = userRepository.save(
                User(
                    username = "user1",
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
                    reviewer = user1,
                    dueDate = LocalDate.now(),
                    completed = true,
                )
            )
        }

        mockMvc.patch("/sheets/${sheet1.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "dueDate": "${LocalDate.now()}"
                }
            """.trimIndent()
        }.andExpect {
            status { isForbidden }
        }

        AdminAuthToken().setAsCurrentAuthInContext().use {
            assertThat(sheetRepository.findById(sheet1.id!!))
                .get()
                .isEqualTo(sheet1)
        }
    }
}
