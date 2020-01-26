package org.polycreo.tests.integration

import assertk.assertions.isEqualTo
import org.flywaydb.test.FlywayTestExecutionListener
import org.flywaydb.test.annotation.FlywayTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
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
interface ReadableIntegrationTest<T : Any> : AbstractIdentifyIntegrationTest<T> {

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
     * Assert [created][create] resource [body].
     */
    fun assertResource(body: String)

    /**
     * Assert resource with [id] is absent.
     */
    fun assertAbsent(id: Any) {
        // exercise
        val actual = testRestTemplate.getForEntity("$path/$id", String::class.java)
        // verify
        ResponseEntityAsserts.assertNotFound(actual) { it.isEqualTo("Failed to get: $id not found") }
    }

    /**
     * Test for Get API.
     */
    @Test
    @FlywayTest
    fun testGet() {
        // setup
        val created: T = create()
        // exercise
        val actual = testRestTemplate.getForEntity("$path/${created.id}", String::class.java)
        // verify
        ResponseEntityAsserts.assertOk(actual)
        assertResource(actual.body!!)
    }

    /**
     * Test for Get absent resource.
     *
     * Expected: 404 Not Found
     */
    @Test
    @FlywayTest
    fun testGet_Absent_404() {
        assertAbsent("absent")
    }
}
