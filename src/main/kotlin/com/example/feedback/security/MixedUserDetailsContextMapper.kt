package com.example.feedback.security

import com.example.feedback.models.User
import com.example.feedback.services.UserService
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.LdapUserDetails
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper
import org.springframework.stereotype.Component

@Component
class MixedUserDetailsContextMapper(
    private val userService: UserService
) : LdapUserDetailsMapper() {

    class MixedUserDetailsEssence(
        private val user: User,
        ldapUserDetails: LdapUserDetails,
    ) : LdapUserDetailsImpl.Essence(ldapUserDetails) {

        override fun createTarget(): MixedUserDetails {
            return MixedUserDetails()
        }

        override fun createUserDetails(): MixedUserDetails {
            addAuthority(user.userRole)

            val details = super.createUserDetails() as MixedUserDetails

            return details.apply {
                id = user.id
                department = user.department
                appointment = user.appointment
            }
        }
    }

    // TODO how does this thing called only on login?
    override fun mapUserFromContext(
        ctx: DirContextOperations,
        username: String,
        authorities: MutableCollection<out GrantedAuthority>
    ): UserDetails {

        val actualUsername = ctx.getStringAttribute("samaccountname")
        val fullName = ctx.getStringAttribute("displayName")
        val email = ctx.getStringAttribute("mail") ?: ""
        val department = ctx.dn.toString().substringAfter("OU=").substringBefore(",")
        val appointment = ctx.getStringAttribute("title") ?: ""

        val user = AdminAuthToken().setAsCurrentAuthInContext().use {
            userService.updateOrCreateDefault(
                actualUsername,
                fullName = fullName,
                email = email,
                department = department,
                appointment = appointment,
            )
        }

        val ldapUserDetails = super.mapUserFromContext(ctx, actualUsername, authorities)

        return MixedUserDetailsEssence(user, ldapUserDetails as LdapUserDetails)
            .createUserDetails()
    }
}
