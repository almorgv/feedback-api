package com.example.feedback.services

import com.example.feedback.models.User
import com.example.feedback.models.UserRole
import com.example.feedback.repositories.UserRepository
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {

    fun updateOrCreateDefault(
        username: String,
        fullName: String,
        email: String,
        department: String,
        appointment: String,
        active: Boolean = true,
    ): User {
        return userRepository.findByUsername(username)
            ?.let {
                it.fullName = fullName
                it.email = email
                it.department = department
                it.appointment = appointment
                it.active = active
                userRepository.save(it)
            }
            ?: createDefault(
                username,
                fullName,
                email,
                department,
                appointment,
            )
    }

    fun createDefault(
        username: String,
        fullName: String,
        email: String,
        department: String,
        appointment: String,
    ): User {
        val user = User(
            username = username,
            userRole = getDefaultRole(),
            fullName = fullName,
            email = email,
            department = department,
            appointment = appointment,
        )
        return userRepository.save(user)
    }

    /**
     * Returns admin role if no admin users exist
     * Otherwise returns user role
     */
    fun getDefaultRole(): UserRole {
        return if (userRepository.existsByUserRole(UserRole.ROLE_ADMIN)) {
            UserRole.ROLE_USER
        } else {
            UserRole.ROLE_ADMIN
        }
    }
}
