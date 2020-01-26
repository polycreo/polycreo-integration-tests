package org.polycreo.tests.integration.oauth2

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class BearerTokenAuthenticationInterceptor(var bearerTokenSupplier: () -> String?) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        if (request.headers.containsKey(HttpHeaders.AUTHORIZATION) == false) {
            bearerTokenSupplier()?.let {
                request.headers.setBearerAuth(it)
            }
        }
        return execution.execute(request, body)
    }
}
