package org.polycreo.tests.integration

import org.flywaydb.test.FlywayTestExecutionListener
import org.flywaydb.test.annotation.FlywayTest
import org.junit.jupiter.api.Test
import org.polycreo.oauth2.DummyOpaqueTokenAuthenticationProvider
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener

/**
 * Integration test for TruncatableController.
 */
@ActiveProfiles("test")
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    FlywayTestExecutionListener::class
)
@Suppress("FunctionNaming", "MagicNumber")
interface TruncatableIntegrationTest<T : Any> {

    /**
     * Root path for the resource.
     */
    val path: String

    /**
     * Access token to use for each requests.
     */
    var accessToken: String?

    /**
     * [TestRestTemplate] to use for each requests.
     */
    var testRestTemplate: TestRestTemplate

    /**
     * Create multiple resources for test preparation.
     */
    fun create(size: Int): Iterable<T>

    /**
     * Test for Truncate API.
     *
     * Precondition: empty
     */
    @Test
    @FlywayTest
    fun testTruncate_Empty() {
        // exercise
        val actual = testRestTemplate.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
        // verify
        ResponseEntityAsserts.assertNoContent(actual)
    }

    /**
     * Test for Truncate API.
     *
     * Precondition: 10 resources exist
     */
    @Test
    @FlywayTest
    fun testTruncate() {
        // setup
        create(10)
        // exercise
        val actual = testRestTemplate.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
        // verify
        ResponseEntityAsserts.assertNoContent(actual)
    }

    /**
     * Test for Truncate API.
     *
     * Precondition: without access token
     *
     * Expected: 401 Unauthorized
     */
    @Test
    @FlywayTest
    fun testTruncate_WithoutToken_401() {
        // setup
        accessToken = null
        // exercise
        val actual = testRestTemplate.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
        // verify
        ResponseEntityAsserts.assertUnauthorized(actual)
    }

    /**
     * Test for Truncate API.
     *
     * Precondition: without `ROOT` or `*:Truncate*` authority
     *
     * Expected: 403 Forbidden
     */
    @Test
    @FlywayTest
    fun testTruncate_WithoutAuthority_403() {
        // setup
        accessToken = DummyOpaqueTokenAuthenticationProvider.createDummyToken(
            "example-user", arrayOf("ACTUATOR"), arrayOf("openid", "profile"), System.currentTimeMillis(), 60000)

        // exercise
        val actual = testRestTemplate.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
        // verify
        ResponseEntityAsserts.assertForbidden(actual)
    }
}
