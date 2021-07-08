package com.example.feedback.exceptions

import org.springframework.context.MessageSource
import org.springframework.data.rest.webmvc.RepositoryRestExceptionHandler
import org.springframework.data.rest.webmvc.support.ExceptionMessage
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.validation.ValidationException

@ControllerAdvice
class ExceptionAdvisor(messageSource: MessageSource) : RepositoryRestExceptionHandler(messageSource) {

    @ExceptionHandler(
        ValidationException::class,
    )
    fun badRequestHandler(ex: Exception): ResponseEntity<*> {
        return ResponseEntity(ExceptionMessage(ex), HttpHeaders(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(
        PreconditionException::class,
    )
    fun preconditionFailedHandler(ex: Exception): ResponseEntity<*> {
        return ResponseEntity(ExceptionMessage(ex), HttpHeaders(), HttpStatus.PRECONDITION_FAILED)
    }
}
