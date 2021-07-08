package com.example.feedback.events

import com.example.feedback.getEntityId
import com.example.feedback.models.JobRole
import com.example.feedback.models.Position
import com.example.feedback.models.Review
import com.example.feedback.models.Sheet
import com.example.feedback.models.User
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
import org.junit.jupiter.params.provider.EnumSource
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

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
internal class ReviewEventHandlerTest(
    @Autowired private val sheetRepository: SheetRepository,
    @Autowired private val reviewRepository: ReviewRepository,
    @Autowired private val userRepository: UserRepository,
    @Autowired private val jobRoleRepository: JobRoleRepository,
    @Autowired private val mockMvc: MockMvc,
) {

    @AfterEach
    fun afterEach() {
        AdminAuthToken().setAsCurrentAuthInContext().use {
            sheetRepository.deleteAll()
            reviewRepository.deleteAll()
            userRepository.deleteAll()
            jobRoleRepository.deleteAll()
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should fail to create review for user without jobRole`() {
        var user1: User

        AdminAuthToken().setAsCurrentAuthInContext().use {
            user1 = userRepository.save(
                User(
                    username = "user1",
                )
            )
        }

        mockMvc.post("/reviews") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "user": "/users/${user1.id}",
                    "period": "Q1"
                }
            """.trimIndent()
        }.andExpect {
            status { isPreconditionFailed }
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should complete all sheets on review completion`() {
        var review1: Review

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(
                JobRole(
                    name = "jobRole1",
                )
            )
            val user1 = userRepository.save(
                User(
                    username = "user1",
                    jobRole = jobRole1,
                )
            )
            val user2 = userRepository.save(
                User(
                    username = "user2",
                    jobRole = jobRole1,
                )
            )
            val user3 = userRepository.save(
                User(
                    username = "user3",
                    jobRole = jobRole1,
                )
            )
            val user4 = userRepository.save(
                User(
                    username = "user4",
                    jobRole = jobRole1,
                )
            )
            review1 = reviewRepository.save(
                Review(
                    user = user1,
                    period = "Q1",
                )
            )
            sheetRepository.saveAll(
                listOf(
                    Sheet(
                        review = review1,
                        reviewer = user2,
                        dueDate = LocalDate.now()
                    ),
                    Sheet(
                        review = review1,
                        reviewer = user3,
                        dueDate = LocalDate.now()
                    ),
                    Sheet(
                        review = review1,
                        reviewer = user4,
                        dueDate = LocalDate.now()
                    ),
                )
            )
        }

        mockMvc.patch("/reviews/${review1.id}") {
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
            assertThat(reviewRepository.findById(review1.id!!))
                .get()
                .extracting { it.sheets }
                .asInstanceOf(InstanceOfAssertFactories.list(Sheet::class.java))
                .allMatch {
                    it.completed
                }
                .allMatch {
                    it.completedDate!!.isAfter(Instant.now().minusSeconds(10))
                }
                .allMatch {
                    it.completedDate!!.isBefore(Instant.now())
                }
        }
    }

    @ParameterizedTest
    @EnumSource(value = Position::class)
    @WithMockUser(roles = ["HEAD"])
    fun `should set current user position on review creation`(position: Position) {
        val user1: User

        AdminAuthToken().setAsCurrentAuthInContext().use {
            val jobRole1 = jobRoleRepository.save(JobRole(name = "jobRole1"))
            user1 = userRepository.save(User(username = "user1", jobRole = jobRole1, position = position))
        }

        val res = mockMvc.post("/reviews") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "user": "/users/${user1.id}",
                    "period": "Q1"
                }
            """.trimIndent()
        }.andExpect {
            status { if (position != Position.NONE) isCreated else isPreconditionFailed }
        }.andReturn()

        if (position != Position.NONE) {
            AdminAuthToken().setAsCurrentAuthInContext().use {
                assertThat(reviewRepository.findById(res.getEntityId()))
                    .get()
                    .extracting { it.userPosition }
                    .isEqualTo(user1.position)
            }
        }
    }

    @Test
    @WithMockUser(roles = ["HEAD"])
    fun `should auto create self review`() {
        val user1: User

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
                    position = Position.MIDDLE,
                )
            )
        }

        val res = mockMvc.post("/reviews") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                    "user": "/users/${user1.id}",
                    "period": "Q1"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated }
        }.andReturn()

        AdminAuthToken().setAsCurrentAuthInContext().use {
            assertThat(reviewRepository.findById(res.getEntityId()))
                .get()
                .extracting { it.selfReview?.review?.user?.username }
                .isEqualTo(user1.username)
        }
    }
}
