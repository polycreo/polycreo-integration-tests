package org.polycreo.tests.integration

import assertk.all
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import java.net.URI
import org.flywaydb.test.FlywayTestExecutionListener
import org.flywaydb.test.annotation.FlywayTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener

private val logger = mu.KotlinLogging.logger {}

/**
 * Integration test for CreatableController.
 */
@ActiveProfiles("test")
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    FlywayTestExecutionListener::class
)
@Suppress("FunctionNaming")
interface CreatableIntegrationTest {

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
     * [CreateRequest] to create resource.
     */
    val successfulCreateRequest: Any

    /**
     * [CreateRequest] (or [Map]) that should be failed to create.
     */
    val invalidCreateRequests: Map<String, Any>

    /**
     * Assert successful `201 Created` response location header and body.
     */
    fun assertCreatedResource(location: URI, body: String)

    /**
     * Test for Create API.
     *
     * Expected: 201 Created
     */
    @Test
    @FlywayTest
    fun testCreate() {
        // exercise
        val actual = testRestTemplate.postForEntity(path, successfulCreateRequest, String::class.java)
        // verify
        ResponseEntityAsserts.assertCreated(actual)
        assertCreatedResource(actual.headers.location!!, actual.body!!)
    }

    /**
     * Test for Create API result in conflict.
     *
     * Expected: 409 Conflict
     */
    @Test
    @FlywayTest
    fun testCreate_SameCode_409() {
        // setup
        testRestTemplate.postForEntity(path, successfulCreateRequest, String::class.java)
        // exercise
        val actual = testRestTemplate.postForEntity(path, successfulCreateRequest, String::class.java)
        // verify
        ResponseEntityAsserts.assertConflict(actual) {
            it.all {
                startsWith("The ID of the entity")
                endsWith(" is duplicated.")
            }
        }
    }

    /**
     * Test for Create API result in bad request.
     *
     * Expected: 400 Bad Request
     */
    @Test
    @FlywayTest
    fun testCreate_InvalidRequest_400() {
        invalidCreateRequests.forEach { (desc, req) ->
            // setup
            logger.info { "Start $desc" }
            // exercise
            val actual = testRestTemplate.postForEntity(path, req, String::class.java)
            // verify
            logger.info { actual }
            assertThat(actual.statusCode, desc).isEqualTo(HttpStatus.BAD_REQUEST)
            if (desc == "required missing") {
                ResponseEntityAsserts.assertBadRequest(actual) { /* only assert isString */ }
            } else {
                ResponseEntityAsserts.assertBadRequestConstraintViolation(actual)
            }
        }
    }
}
