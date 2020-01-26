/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.polycreo.tests.integration

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import assertk.assertions.isNull
import org.polycreo.kotlin.assertkjson.isInt
import org.polycreo.kotlin.assertkjson.isString
import org.polycreo.kotlin.assertkjson.jsonNodeOf
import org.polycreo.kotlin.assertkjson.jsonPath
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

private val logger = mu.KotlinLogging.logger {}

typealias StringAssertion = ((Assert<String>) -> Unit)?

/**
 * Test assertions for [ResponseEntity].
 */
object ResponseEntityAsserts {

    fun commonAssertion(actual: ResponseEntity<String>, expectedStatus: HttpStatus? = null) {
        logger.info { actual }
        assertThat(actual.headers.filterKeys { it == "Set-Cookie" }).isEmpty()
        expectedStatus?.let { assertThat(actual.statusCode).isEqualTo(it) }
    }

    // 2xx

    fun assertOk(actual: ResponseEntity<String>) {
        commonAssertion(actual, HttpStatus.OK)
    }

    fun assertCreated(actual: ResponseEntity<String>) {
        commonAssertion(actual, HttpStatus.CREATED)
    }

    fun assertNoContent(actual: ResponseEntity<String>) {
        commonAssertion(actual, HttpStatus.NO_CONTENT)
        assertThat(actual.body).isNull()
    }

    // 4xx

    fun assertBadRequest(actual: ResponseEntity<String>, detailAssertion: StringAssertion = null) {
        commonAssertion(actual, HttpStatus.BAD_REQUEST)
        assertThat(jsonNodeOf(actual.body!!)).all {
            jsonPath("$.title").isString().isEqualTo("Bad Request")
            jsonPath("$.status").isInt().isEqualTo(HttpStatus.BAD_REQUEST.value())
            detailAssertion?.let { it ->
                jsonPath("$.detail").isString().also { value -> it(value) }
            }
        }
    }

    fun assertBadRequestConstraintViolation(actual: ResponseEntity<String>, detailAssertion: StringAssertion = null) {
        commonAssertion(actual, HttpStatus.BAD_REQUEST)
        assertThat(jsonNodeOf(actual.body!!)).all {
            jsonPath("$.type").isString().isEqualTo("https://zalando.github.io/problem/constraint-violation")
            jsonPath("$.title").isString().isEqualTo("Constraint Violation")
            jsonPath("$.status").isInt().isEqualTo(HttpStatus.BAD_REQUEST.value())
            detailAssertion?.let { it ->
                jsonPath("$.detail").isString().also { value -> it(value) }
            }
        }
    }

    fun assertUnauthorized(actual: ResponseEntity<String>) {
        commonAssertion(actual, HttpStatus.UNAUTHORIZED)
        assertThat(jsonNodeOf(actual.body!!)).all {
            jsonPath("$.title").isString().isEqualTo("Unauthorized")
            jsonPath("$.status").isInt().isEqualTo(HttpStatus.UNAUTHORIZED.value())
            jsonPath("$.detail").isString().isIn(
                "Full authentication is required to access this resource",
                "このリソースにアクセスするには認証をする必要があります"
            )
        }
    }

    fun assertForbidden(actual: ResponseEntity<String>) {
        commonAssertion(actual, HttpStatus.FORBIDDEN)
        assertThat(jsonNodeOf(actual.body!!)).all {
            jsonPath("$.title").isString().isEqualTo("Forbidden")
            jsonPath("$.status").isInt().isEqualTo(HttpStatus.FORBIDDEN.value())
            jsonPath("$.detail").isString().isIn(
                "Access is denied",
                "アクセスが拒否されました"
            )
        }
    }

    fun assertNotFound(actual: ResponseEntity<String>, detailAssertion: StringAssertion = null) {
        commonAssertion(actual, HttpStatus.NOT_FOUND)
        assertThat(jsonNodeOf(actual.body!!)).all {
            jsonPath("$.title").isString().isEqualTo("Not Found")
            jsonPath("$.status").isInt().isEqualTo(HttpStatus.NOT_FOUND.value())
            detailAssertion?.let { it ->
                jsonPath("$.detail").isString().also { value -> it(value) }
            }
        }
    }

    fun assertConflict(actual: ResponseEntity<String>, detailAssertion: StringAssertion = null) {
        commonAssertion(actual, HttpStatus.CONFLICT)
        assertThat(jsonNodeOf(actual.body!!)).all {
            jsonPath("$.title").isString().isEqualTo("Conflict")
            jsonPath("$.status").isInt().isEqualTo(HttpStatus.CONFLICT.value())
            detailAssertion?.let { it ->
                jsonPath("$.detail").isString().also { value -> it(value) }
            }
        }
    }
}
