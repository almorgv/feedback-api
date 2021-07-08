package com.example.feedback.security

import org.springframework.boot.autoconfigure.ldap.LdapProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val ldapProperties: LdapProperties,
    private val userDetailsContextMapper: MixedUserDetailsContextMapper,
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http {
            // TODO oauth
            formLogin {
                loginProcessingUrl = "/login"
                authenticationSuccessHandler = ResponseAuthenticationSuccessHandler()
                authenticationFailureHandler = ResponseAuthenticationFailureHandler()
            }
            csrf {
                disable()
            }
            logout {
                logoutUrl = "/logout"
                deleteCookies("JSESSIONID")
            }
            authorizeRequests {
                authorize("/login", permitAll)
                authorize("/logout", permitAll)
                authorize("/actuator/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-resources/**", permitAll)
                authorize("/v3/api-docs", permitAll)
                authorize(anyRequest, authenticated)
            }
            sessionManagement {
                sessionAuthenticationFailureHandler = ResponseAuthenticationFailureHandler()
                invalidSessionStrategy = ResponseInvalidSessionStrategy()
            }
            exceptionHandling {
                authenticationEntryPoint = Http403ForbiddenEntryPoint()
            }
        }
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        // TODO move domain to properties
        ActiveDirectoryLdapAuthenticationProvider("svadom.local", ldapProperties.urls.first())
            .apply {
                setConvertSubErrorCodesToExceptions(true)
                setUseAuthenticationRequestCredentials(true)
                setUserDetailsContextMapper(userDetailsContextMapper)
                auth.authenticationProvider(this)
            }
    }
}
