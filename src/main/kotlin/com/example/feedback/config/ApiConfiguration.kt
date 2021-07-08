package com.example.feedback.config

import com.example.feedback.models.User
import org.reflections.Reflections
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer
import org.springframework.http.HttpMethod
import javax.persistence.Entity

@Configuration
@ConditionalOnProperty(name = ["app.api.allow-manually-users-creation"], havingValue = "false", matchIfMissing = true)
class ApiExposureAllowManualUsersCreationConfiguration : RepositoryRestConfigurer {
    override fun configureRepositoryRestConfiguration(config: RepositoryRestConfiguration) {
        config.exposureConfiguration
            .forDomainType(User::class.java)
            .disablePutForCreation()
            .withCollectionExposure { _, httpMethods -> httpMethods.disable(HttpMethod.POST) }
    }
}

@Configuration
class ApiConfiguration : RepositoryRestConfigurer {
    override fun configureRepositoryRestConfiguration(config: RepositoryRestConfiguration) {
        val modelClasses = Reflections("com.example.feedback.models")
            .getTypesAnnotatedWith(Entity::class.java)

        config
            .exposeIdsFor(*modelClasses.toTypedArray())
    }
}
