package com.example.feedback.security

import com.example.feedback.models.BaseEntityId
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl

class MixedUserDetails(
    var id: BaseEntityId? = null,
    var department: String = "",
    var appointment: String = "",
) : LdapUserDetailsImpl()
