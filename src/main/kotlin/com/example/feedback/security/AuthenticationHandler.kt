package com.example.feedback.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.rest.webmvc.support.ExceptionMessage
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.session.SessionAuthenticationException
import org.springframework.security.web.session.InvalidSessionStrategy
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val objectMapper = ObjectMapper()

private fun sendAuthError(
    response: HttpServletResponse,
    exception: AuthenticationException
) {
    response.status = HttpStatus.UNAUTHORIZED.value()
    response.contentType = "${MediaType.APPLICATION_JSON_VALUE}; charset=${Charsets.UTF_8.name()}"

    val data = ExceptionMessage(exception)

    response.writer.println(objectMapper.writeValueAsString(data))
}

class ResponseAuthenticationSuccessHandler : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        response.status = HttpStatus.OK.value()
    }
}

class ResponseAuthenticationFailureHandler : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        sendAuthError(response, exception)
    }
}

class ResponseInvalidSessionStrategy : InvalidSessionStrategy {
    override fun onInvalidSessionDetected(request: HttpServletRequest, response: HttpServletResponse) {
        sendAuthError(response, SessionAuthenticationException("Invalid or expired session"))
    }
}
