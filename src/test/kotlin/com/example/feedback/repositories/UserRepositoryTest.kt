// package com.example.feedback.controllers
//
// import com.fasterxml.jackson.databind.ObjectMapper
// import com.example.feedback.models.Position
// import com.example.feedback.models.Review
// import com.example.feedback.models.User
// import com.example.feedback.repositories.UserRepository
// import org.assertj.core.api.Assertions.assertThat
// import org.hamcrest.Matchers.equalTo
// import org.hamcrest.Matchers.notNullValue
// import org.jeasy.random.EasyRandom
// import org.jeasy.random.EasyRandomParameters
// import org.jeasy.random.FieldPredicates.isAnnotatedWith
// import org.jeasy.random.FieldPredicates.named
// import org.junit.jupiter.api.AfterEach
// import org.junit.jupiter.api.Test
// import org.springframework.beans.factory.annotation.Autowired
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
// import org.springframework.boot.test.context.SpringBootTest
// import org.springframework.http.MediaType
// import org.springframework.test.context.ActiveProfiles
// import org.springframework.test.web.servlet.MockMvc
// import org.springframework.test.web.servlet.delete
// import org.springframework.test.web.servlet.get
// import org.springframework.test.web.servlet.post
// import org.springframework.test.web.servlet.put
// import java.time.Instant
// import javax.persistence.Id
// import javax.persistence.ManyToOne
// import kotlin.streams.toList
//
// @SpringBootTest
// @ActiveProfiles("test")
// @AutoConfigureMockMvc
// class UsersControllerTest(
//     @Autowired private val mockMvc: MockMvc,
//     @Autowired private val userRepository: UserRepository,
//     @Autowired private val objectMapper: ObjectMapper,
// ) {
//
//     private val random = EasyRandom(
//         EasyRandomParameters()
//             .excludeField(
//                 isAnnotatedWith(
//                     Id::class.java,
//                     ManyToOne::class.java,
//                 )
//             )
//             .randomize(
//                 named("reviews"),
//                 { mutableListOf<Review>() }
//             )
//     )
//
//     @AfterEach
//     fun afterEach() {
//         userRepository.deleteAll()
//     }
//
//     @Test
//     fun `should list`() {
//         val users = random.objects(User::class.java, 3).toList()
//         userRepository.saveAll(users)
//         mockMvc.get("/users")
//             .andExpect {
//                 status { isOk }
//                 content {
//                     contentType(MediaType.APPLICATION_JSON)
//                 }
//                 jsonPath("$.length()", equalTo(users.size))
//             }
//     }
//
//     @Test
//     fun `should get`() {
//         val users = random.objects(User::class.java, 3).toList()
//         userRepository.saveAll(users)
//         val user = users.random()
//         mockMvc.get("/users/{id}", user.id)
//             .andExpect {
//                 status { isOk }
//                 content {
//                     contentType(MediaType.APPLICATION_JSON)
//                 }
//                 jsonPath("$.id", equalTo(user.id?.toInt()))
//                 jsonPath("$.username", equalTo(user.username))
//             }
//     }
//
//     @Test
//     fun `should not get nonexistent id`() {
//         mockMvc.get("/users/{id}", 123)
//             .andExpect {
//                 status { isNotFound }
//                 content {
//                     contentType(MediaType.APPLICATION_JSON)
//                 }
//                 jsonPath("$.message", notNullValue())
//             }
//     }
//
//     @Test
//     fun `should create`() {
//         val users = random.objects(User::class.java, 3).toList()
//
//         users.forEach {
//             mockMvc.post("/users") {
//                 contentType = MediaType.APPLICATION_JSON
//                 content = objectMapper.writeValueAsString(it)
//             }
//                 .andExpect {
//                     status { isCreated }
//                     content {
//                         contentType(MediaType.APPLICATION_JSON)
//                     }
//                     jsonPath("$.id", notNullValue())
//                     jsonPath("$.username", equalTo(it.username))
//                 }
//         }
//
//         val savedUsers = userRepository.findAll()
//         assertThat(savedUsers)
//             .hasSize(users.size)
//             .usingElementComparatorIgnoringFields(
//                 "id",
//                 User::reviews.name,
//                 User::createdDate.name,
//                 User::lastModifiedDate.name
//             )
//             .containsExactlyInAnyOrderElementsOf(users)
//     }
//
//     @Test
//     fun `create should set audit info`() {
//         val users = random.objects(User::class.java, 3).toList()
//
//         users.forEach {
//             mockMvc.post("/users") {
//                 contentType = MediaType.APPLICATION_JSON
//                 content = objectMapper.writeValueAsString(it)
//             }
//                 .andExpect {
//                     status { isCreated }
//                 }
//         }
//
//         val savedUsers = userRepository.findAll()
//         assertThat(savedUsers)
//             .allMatch {
//                 it.createdDate
//                     .run {
//                         isAfter(Instant.now().minusSeconds(60)) &&
//                             isBefore(Instant.now().plusSeconds(60))
//                     }
//                 it.lastModifiedDate
//                     .run {
//                         isAfter(Instant.now().minusSeconds(60)) &&
//                             isBefore(Instant.now().plusSeconds(60))
//                     }
//             }
//     }
//
//     @Test
//     fun `create should fail to deserialize bad values`() {
//         mockMvc.post("/users") {
//             contentType = MediaType.APPLICATION_JSON
//             content = """
//                 {
//                     "username": "asd",
//                     "role": "ROLEFAIL",
//                     "position": "POSITIONFAIL",
//                 }
//             """.trimIndent()
//         }
//             .andExpect {
//                 status { is4xxClientError }
//                 content {
//                     contentType(MediaType.APPLICATION_JSON)
//                 }
//                 jsonPath("$.message", notNullValue())
//             }
//
//         assertThat(userRepository.findAll())
//             .isEmpty()
//     }
//
//     @Test
//     fun `should update`() {
//         val user = random.nextObject(User::class.java)
//         userRepository.save(user)
//
//         user.apply {
//             position = Position.SENIOR
//             username = "asd"
//         }
//
//         mockMvc.put("/users/{id}", user.id) {
//             contentType = MediaType.APPLICATION_JSON
//             content = objectMapper.writeValueAsString(user)
//         }
//             .andExpect {
//                 status { isOk }
//                 content {
//                     contentType(MediaType.APPLICATION_JSON)
//                 }
//                 jsonPath("$.id", equalTo(user.id?.toInt()))
//                 jsonPath("$.position", equalTo(user.position.toString()))
//                 jsonPath("$.username", equalTo(user.username))
//                 jsonPath("$.role", equalTo(user.role.toString()))
//             }
//
//         val savedUser = userRepository.findById(user.id!!)
//         assertThat(savedUser)
//             .get()
//             .isEqualTo(user)
//         assertThat(savedUser.get().lastModifiedDate)
//             .isAfter(user.lastModifiedDate)
//     }
//
//     @Test
//     fun `should delete`() {
//         val users = random.objects(User::class.java, 3).toList()
//         userRepository.saveAll(users)
//
//         val userToDelete = users.random()
//
//         mockMvc.delete("/users/{id}", userToDelete.id)
//             .andExpect {
//                 status { isOk }
//             }
//
//         assertThat(userRepository.findAll())
//             .hasSize(users.size - 1)
//             .containsExactlyInAnyOrderElementsOf(users.filter { it.id != userToDelete.id })
//     }
// }
