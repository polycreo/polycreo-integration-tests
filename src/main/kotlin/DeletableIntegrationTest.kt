package org.polycreo.tests.integration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.flywaydb.test.FlywayTestExecutionListener
import org.flywaydb.test.annotation.FlywayTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener

/**
 * Integration test for ReadableController.
 */
@ActiveProfiles("test")
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    FlywayTestExecutionListener::class
)
@Suppress("FunctionNaming")
interface DeletableIntegrationTest<T : Any> : AbstractIdentifyIntegrationTest<T> {

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
     * Create a resource for test preparation.
     */
    fun create(): T

    /**
     * Assert deleted resource.
     */
    fun assertResource(body: String)

    /**
     * Assert resource is absent.
     */
    fun assertAbsent(id: Any)

    /**
     * Test for Get API.
     */
    @Test
    @FlywayTest
    fun testDelete() {
        // setup
        val created: T = create()
        // exercise
        val actual = testRestTemplate.exchange(
            "$path/${created.id}", HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
        // verify
        ResponseEntityAsserts.commonAssertion(actual)
        when (actual.statusCode) {
            HttpStatus.OK -> assertResource(actual.body!!)
            HttpStatus.NO_CONTENT -> assertThat(actual.body).isNull()
            else -> throw AssertionError("Unexpected status code: ${actual.statusCode}")
        }
        assertAbsent(created.id!!)
    }

    /**
     * Test for Get absent resource.
     *
     * Expected: 404 Not Found
     */
    @Test
    @FlywayTest
    fun testDelete_Absent_404() {
        // exercise
        val actual = testRestTemplate.exchange("$path/absent", HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
        // verify
        ResponseEntityAsserts.assertNotFound(actual) { it.isEqualTo("Failed to delete: absent not found") }
    }
}
