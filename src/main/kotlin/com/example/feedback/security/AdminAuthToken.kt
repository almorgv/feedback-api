package com.example.feedback.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.ldap.userdetails.Person
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import java.io.Closeable

class AdminAuthToken : Closeable {
    private val authHolder = ThreadLocal<Authentication>()
    private var isSet = false

    fun setAsCurrentAuthInContext(): AdminAuthToken {
        val securityContext = SecurityContextHolder.getContext()
        authHolder.set(securityContext.authentication)
        val authorityList = AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        val principal = Person.Essence()
            .apply {
                setUsername("APP")
                setDn("")
                setCn(arrayOf(""))
            }.createUserDetails()
        val token = PreAuthenticatedAuthenticationToken(principal, "ROLE_ADMIN", authorityList)
        securityContext.authentication = token
        isSet = true
        return this
    }

    override fun close() {
        if (isSet) {
            SecurityContextHolder.getContext().authentication = authHolder.get()
        }
    }
}
