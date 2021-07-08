package com.example.feedback.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.annotation.Order
import org.springframework.data.jpa.projection.CollectionAwareProjectionFactory
import org.springframework.data.projection.ProjectionFactory
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.RepresentationModelProcessor
import org.springframework.stereotype.Component

@Component
class ProjectingResourceProcessor(
    private val repositoryRestConfiguration: RepositoryRestConfiguration,
    private val projectionFactory: ProjectionFactory,
    private val securityExpressionChecker: SecurityExpressionChecker,
) : RepresentationModelProcessor<EntityModel<Any>> {

    override fun process(model: EntityModel<Any>): EntityModel<Any> {
        val projection = findSuitableProjection(model) ?: return model

        val projectedEntity = projectionFactory
            .createProjection(projection, model.content!!)

        return EntityModel.of(projectedEntity)
    }

    private fun findSuitableProjection(model: EntityModel<Any>) =
        getProjectionsFor(model)
            .values
            .filter {
                securityExpressionChecker.isPreAndPostAuthorizePassed(it, model.content)
            }
            .minByOrNull {
                AnnotationUtils.findAnnotation(it, Order::class.java)?.value ?: Ordered.LOWEST_PRECEDENCE
            }

    private fun getProjectionsFor(model: EntityModel<Any>) =
        repositoryRestConfiguration
            .projectionConfiguration
            .getProjectionsFor(model.content?.javaClass)
}

@Configuration
class ProjectionConfiguration {
    @Bean
    fun collectionAwareProjectionFactory() = CollectionAwareProjectionFactory()
}
