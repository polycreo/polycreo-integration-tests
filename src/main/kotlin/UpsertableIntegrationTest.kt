package org.polycreo.tests.integration

import java.net.URI
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
 * Integration test for UpsertableController.
 */
@ActiveProfiles("test")
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    FlywayTestExecutionListener::class
)
@Suppress("FunctionNaming")
interface UpsertableIntegrationTest<T> {

    /**
     * Root path for the resource.
     */
    val path: String

    /**
     * Single resource path for the [created][create] resource.
     */
    val targetPath: String

    /**
     * Access token to use for each requests.
     */
    var accessToken: String?

    /**
     * [TestRestTemplate] to use for each requests.
     */
    var testRestTemplate: TestRestTemplate

    /**
     * Upsert request to create resource.
     */
    val successfulUpsertCreateRequest: Any

    /**
     * Upsert request to update [created][create] resource.
     */
    val successfulUpsertUpdateRequest: Any

    /**
     * Upsert request (or [Map]) that should be failed to upsert.
     */
    val invalidUpsertRequests: Map<String, Any>

    /**
     * Create a resource for test preparation.
     */
    fun create(): T

    /**
     * Assert [created][successfulUpsertCreateRequest] resource.
     */
    fun assertUpsertCreatedResource(location: URI, body: String)

    /**
     * Assert [updated][successfulUpsertUpdateRequest] resource.
     */
    fun assertUpsertUpdatedResource(body: String)

    /**
     * Test for Upsert API result in Created.
     *
     * Expected: 201 Created
     */
    @Test
    @FlywayTest
    fun testUpsert_Create_201Created() {
        // exercise
        val actual = testRestTemplate.exchange(
            targetPath, HttpMethod.PUT, HttpEntity(successfulUpsertCreateRequest), String::class.java)
        // verify
        ResponseEntityAsserts.assertCreated(actual)
        assertUpsertCreatedResource(actual.headers.location!!, actual.body!!)
    }

    /**
     * Test for Upsert API result in Updated.
     *
     * Expected: 200 OK
     */
    @Test
    @FlywayTest
    fun testUpsert_Update_200Updated() {
        // setup
        create()
        // exercise
        val actual = testRestTemplate.exchange(
            targetPath, HttpMethod.PUT, HttpEntity(successfulUpsertUpdateRequest), String::class.java)
        // verify
        ResponseEntityAsserts.assertOk(actual)
        assertUpsertUpdatedResource(actual.body!!)
    }

    /**
     * Test for Upsert with invalid request.
     *
     * Expected: 400 Bad Request
     */
    @Test
    @FlywayTest
    fun testUpsert_InvalidRequest_400() {
        invalidUpsertRequests.forEach { (desc, req) ->
            // setup
            logger.info { "Start $desc" }
            // exercise
            val actual = testRestTemplate.exchange(
                targetPath, HttpMethod.PUT, HttpEntity(req), String::class.java)
            // verify
            ResponseEntityAsserts.assertBadRequestConstraintViolation(actual)
        }
    }
}
