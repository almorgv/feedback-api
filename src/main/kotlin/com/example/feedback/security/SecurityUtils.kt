package com.example.feedback.security

import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContext

fun SecurityContext.hasRole(role: String): Boolean {
    return AuthorityUtils
        .authorityListToSet(this.authentication.authorities)
        .contains("ROLE_$role")
}

fun SecurityContext.hasAnyRole(vararg roles: String): Boolean {
    return AuthorityUtils
        .authorityListToSet(this.authentication.authorities)
        .any { authorities -> authorities in roles.map { "ROLE_$it" } }
}
