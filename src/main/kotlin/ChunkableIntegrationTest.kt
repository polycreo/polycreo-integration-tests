package org.polycreo.tests.integration

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import org.flywaydb.test.FlywayTestExecutionListener
import org.flywaydb.test.annotation.FlywayTest
import org.junit.jupiter.api.Test
import org.polycreo.kotlin.assertkjson.hasSize
import org.polycreo.kotlin.assertkjson.isArray
import org.polycreo.kotlin.assertkjson.isInt
import org.polycreo.kotlin.assertkjson.isNotDefined
import org.polycreo.kotlin.assertkjson.isString
import org.polycreo.kotlin.assertkjson.jsonNodeOf
import org.polycreo.kotlin.assertkjson.jsonPath
import org.polycreo.oauth2.DummyOpaqueTokenAuthenticationProvider
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener

private val logger = mu.KotlinLogging.logger {}

/**
 * Integration test for ChunkableController.
 */
@ActiveProfiles("test")
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    FlywayTestExecutionListener::class
)
@Suppress("FunctionNaming", "MagicNumber")
interface ChunkableIntegrationTest<T : Any> {

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
     * Test for List API.
     *
     * Precondition: empty
     */
    @Test
    @FlywayTest
    fun testList_Empty() {
        // exercise
        val actual = testRestTemplate.getForEntity(path, String::class.java)
        // verify
        ResponseEntityAsserts.assertOk(actual)
        assertThat(jsonNodeOf(actual.body!!)).all {
            jsonPath("$.chunk.size").isInt().isEqualTo(0)
            jsonPath("$.chunk.pagination_token").isNotDefined()
            jsonPath("$._embedded.elements").isNotDefined()
        }
    }

    /**
     * Test for List API.
     *
     * Precondition: 3 resources exist
     */
    @Test
    @FlywayTest
    fun testList() {
        // setup
        create(3)
        // exercise
        val actual = testRestTemplate.getForEntity(path, String::class.java)
        // verify
        ResponseEntityAsserts.assertOk(actual)
        assertThat(jsonNodeOf(actual.body!!)).all {
            jsonPath("$.chunk.size").isInt().isEqualTo(3)
            jsonPath("$.chunk.pagination_token").isString()
            jsonPath("$._embedded.elements").isArray().hasSize(3)
        }
    }

    /**
     * Test for List API with pagination.
     *
     * Precondition: 3 resources exist, requested by size=2
     */
    @Test
    @FlywayTest
    fun testList_Paged() {
        // setup
        create(3)
        var paginationToken: String? = null
        var requestCount = 0
        var retrievedElementCount = 0
        do {
            val query = "?size=2" + if (paginationToken != null) "&next=$paginationToken" else ""
            // exercise
            val actual = testRestTemplate.getForEntity(path + query, String::class.java)
            // verify
            ResponseEntityAsserts.assertOk(actual)

            requestCount++
            retrievedElementCount += JsonPath.read<Int>(actual.body!!, "$.chunk.size")

            try {
                paginationToken = JsonPath.read<String>(actual.body!!, "$.chunk.pagination_token")
            } catch (e: PathNotFoundException) {
                paginationToken = null
            }
        } while (paginationToken != null)
        assertThat(requestCount).isEqualTo(3)
        assertThat(retrievedElementCount).isEqualTo(3)
    }

    /**
     * Test for List API.
     *
     * Precondition: without access token
     *
     * Expected: 401 Unauthorized
     */
    @Test
    @FlywayTest
    fun testList_WithoutToken_401() {
        // setup
        accessToken = null
        // exercise
        val actual = testRestTemplate.getForEntity(path, String::class.java)
        // verify
        ResponseEntityAsserts.assertUnauthorized(actual)
    }

    /**
     * Test for List API.
     *
     * Precondition: without `ROOT` or `*:List*` authority
     *
     * Expected: 403 Forbidden
     */
    @Test
    @FlywayTest
    fun testList_WithoutAuthority_403() {
        // setup
        accessToken = DummyOpaqueTokenAuthenticationProvider.createDummyToken(
            "example-user", arrayOf("ACTUATOR"), arrayOf("openid", "profile"), System.currentTimeMillis(), 60000)

        // exercise
        val actual = testRestTemplate.getForEntity(path, String::class.java)
        // verify
        ResponseEntityAsserts.assertForbidden(actual)
    }
}
