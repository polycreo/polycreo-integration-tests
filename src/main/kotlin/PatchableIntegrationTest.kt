package org.polycreo.tests.integration

import assertk.assertions.isEqualTo
import org.flywaydb.test.FlywayTestExecutionListener
import org.flywaydb.test.annotation.FlywayTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener

private val logger = mu.KotlinLogging.logger {}

/**
 * Integration test for PatchableController.
 */
@ActiveProfiles("test")
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    FlywayTestExecutionListener::class
)
@Suppress("FunctionNaming")
interface PatchableIntegrationTest<T : Any> : AbstractIdentifyIntegrationTest<T> {

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
     * [Patch request][UpdateRequest] to update [created][create] resource.
     */
    val successfulPatchRequest: Any

    /**
     * [Patch request][UpdateRequest] (or [Map]) that should be failed to update.
     */
    val invalidPatchRequests: Map<String, Any>

    /**
     * Create a resource for test preparation.
     */
    fun create(): T

    /**
     * Assert [patched][successfulPatchRequest] resource.
     */
    fun assertPatchedResource(body: String)

    /**
     * Test for Patch API.
     *
     * Expected: 200 OK
     */
    @Test
    @FlywayTest
    fun testPatch() {
        try {
            // setup
            val created: T = create()
            val entity = HttpEntity(successfulPatchRequest)
            // exercise
            val actual = testRestTemplate.exchange("$path/${created.id}", HttpMethod.PATCH, entity, String::class.java)
            // verify
            ResponseEntityAsserts.assertOk(actual)
            assertPatchedResource(actual.body!!)
        } catch (e: NotImplementedError) {
            // TODO
        }
    }

    /**
     * Test for Patch absent resource.
     *
     * Expected: 404 Not Found
     */
    @Test
    @FlywayTest
    fun testPatch_Absent_404() {
        try {
            // setup
            val entity = HttpEntity(successfulPatchRequest)
            // exercise
            val actual = testRestTemplate.exchange("$path/absent", HttpMethod.PATCH, entity, String::class.java)
            // verify
            ResponseEntityAsserts.assertNotFound(actual) { it.isEqualTo("Failed to update: absent is not found") }
        } catch (e: NotImplementedError) {
            // TODO
        }
    }

    /**
     * Test for Patch with invalid request.
     *
     * Expected: 400 Bad Request
     */
    @Test
    @FlywayTest
    fun testPatch_InvalidRequest_400() {
        val created: T = create()
        invalidPatchRequests.forEach { (desc, req) ->
            // setup
            logger.info { "Start $desc" }
            // exercise
            val actual = testRestTemplate.postForEntity("$path/${created.id}", req, String::class.java)
            // verify
            ResponseEntityAsserts.assertBadRequestConstraintViolation(actual)
        }
    }
}
