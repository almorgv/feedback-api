package com.example.feedback.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.expression.EvaluationContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.security.access.expression.ExpressionUtils
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.util.SimpleMethodInvocation
import org.springframework.stereotype.Component
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod

@Component
class SecurityExpressionChecker(
    private val methodSecurityExpressionHandler: MethodSecurityExpressionHandler,
) {

    private val parser = SpelExpressionParser()
    private val fakeMethodInvocation = SimpleMethodInvocation(
        FakeSecurityObject(),
        FakeSecurityObject::class.declaredFunctions.find { it.name == "fakeMethod" }?.javaMethod
    )

    private class FakeSecurityObject {
        fun fakeMethod() {}
    }

    fun check(expression: String, returnObject: Any? = null): Boolean {
        return evaluateExpression(expression, getContext(returnObject))
    }

    fun isPreAndPostAuthorizePassed(clazz: Class<*>, returnObject: Any? = null): Boolean {
        return isPreAuthorizePassed(clazz, returnObject) &&
            isPostAuthorizePassed(clazz, returnObject)
    }

    fun isPreAuthorizePassed(clazz: Class<*>, returnObject: Any? = null): Boolean {
        return AnnotationUtils.findAnnotation(clazz, PreAuthorize::class.java)
            ?.run { check(value, returnObject) }
            ?: true
    }

    fun isPostAuthorizePassed(clazz: Class<*>, returnObject: Any? = null): Boolean {
        return AnnotationUtils.findAnnotation(clazz, PostAuthorize::class.java)
            ?.run { check(value, returnObject) }
            ?: true
    }

    private fun getContext(returnObject: Any?): EvaluationContext {
        return methodSecurityExpressionHandler
            .createEvaluationContext(SecurityContextHolder.getContext().authentication, fakeMethodInvocation)
            .apply { (rootObject.value as MethodSecurityExpressionOperations).returnObject = returnObject }
    }

    private fun evaluateExpression(value: String, context: EvaluationContext): Boolean {
        return ExpressionUtils.evaluateAsBoolean(parser.parseExpression(value), context)
    }
}

@Configuration
class SecurityExpressionCheckerConfig {
    @Bean
    fun defaultMethodSecurityExpressionHandler() = DefaultMethodSecurityExpressionHandler()
}
