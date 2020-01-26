package org.polycreo.tests.integration

import org.junit.jupiter.api.BeforeEach
import org.polycreo.oauth2.DummyOpaqueTokenAuthenticationProvider
import org.polycreo.tests.integration.oauth2.BearerTokenAuthenticationInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

/**
 * Skeleton implementation of OAuth2 Access Token management.
 */
@Suppress("UnnecessaryAbstractClass")
@Import(AbstractOAuth2IntegrationTest.RestTemplateConfig::class)
abstract class AbstractOAuth2IntegrationTest {

    @Autowired
    private lateinit var restTemplateConfig: RestTemplateConfig

    /**
     * Access token to use for each requests.
     */
    var accessToken: String?
        get() = restTemplateConfig.accessToken
        set(value) {
            restTemplateConfig.accessToken = value
        }

    /**
     * Initialize [accessToken] with default.
     */
    @BeforeEach
    @Suppress("MagicNumber")
    fun setUpDefaultAccessToken() {
        accessToken = DummyOpaqueTokenAuthenticationProvider.createDummyToken(
            "example-user", arrayOf("ROOT"), arrayOf("openid", "profile"), System.currentTimeMillis(), 60000)
    }

    /**
     * Test configuration for the [RestTemplateCustomizer] to use configured access token.
     */
    @TestConfiguration
    class RestTemplateConfig {

        var accessToken: String? = null

        @Bean
        fun restTemplateCustomizer(): RestTemplateCustomizer = RestTemplateCustomizer { restTemplate ->
                restTemplate.interceptors.add(BearerTokenAuthenticationInterceptor { accessToken })
        }
    }
}
