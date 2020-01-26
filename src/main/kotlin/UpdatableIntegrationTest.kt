package org.polycreo.tests.integration

import assertk.assertions.isEqualTo
import org.flywaydb.test.FlywayTestExecutionListener
import org.flywaydb.test.annotation.FlywayTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener

private val logger = mu.KotlinLogging.logger {}

/**
 * Integration test for UpdatableController.
 */
@ActiveProfiles("test")
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    FlywayTestExecutionListener::class
)
@Suppress("FunctionNaming")
interface UpdatableIntegrationTest<T : Any> : AbstractIdentifyIntegrationTest<T> {

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
     * [UpdateRequest] to update [created][create] resource.
     */
    val successfulUpdateRequest: Any

    /**
     * [UpdateRequest] (or [Map]) that should be failed to update.
     */
    val invalidUpdateRequests: Map<String, Any>

    /**
     * Create a resource for test preparation.
     */
    fun create(): T

    /**
     * Assert [updated][successfulUpdateRequest] resource.
     */
    fun assertUpdatedResource(body: String)

    /**
     * Test for Update API.
     *
     * Expected: 200 OK
     */
    @Test
    @FlywayTest
    fun testUpdate() {
        // setup
        val created: T = create()
        // exercise
        val actual = testRestTemplate.postForEntity("$path/${created.id}", successfulUpdateRequest, String::class.java)
        // verify
        ResponseEntityAsserts.assertOk(actual)
        assertUpdatedResource(actual.body!!)
    }

    /**
     * Test for Update absent resource.
     *
     * Expected: 404 Not Found
     */
    @Test
    @FlywayTest
    fun testUpdate_Absent_404() {
        // exercise
        val actual = testRestTemplate.postForEntity("$path/absent", successfulUpdateRequest, String::class.java)
        // verify
        ResponseEntityAsserts.assertNotFound(actual) { it.isEqualTo("Failed to update: absent is not found") }
    }

    /**
     * Test for Update with invalid request.
     *
     * Expected: 400 Bad Request
     */
    @Test
    @FlywayTest
    fun testUpdate_InvalidRequest_400() {
        val created: T = create()
        invalidUpdateRequests.forEach { (desc, req) ->
            // setup
            logger.info { "Start $desc" }
            // exercise
            val actual = testRestTemplate.postForEntity("$path/${created.id}", req, String::class.java)
            // verify
            ResponseEntityAsserts.assertBadRequestConstraintViolation(actual)
        }
    }
}
