package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.Logger
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import java.lang.reflect.Type
import java.net.HttpURLConnection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BaseHttpClient(
    private val connectionCreator: NetworkConnectionCreator,
    private val responseManager: HttpResponseManager,
) : HttpClient {

    @WorkerThread
    @JvmSynthetic
    override fun <T> newCall(request: Request, typeOfT: Type): Response<T> {
        Logger.log(VERBOSE) {
            "${request.method.name} ${request.url}${
                request.body.takeIf(String::isNotEmpty)?.let { body -> " Body: $body" }.orEmpty()
            }"
        }

        var connection: HttpURLConnection? = null

        try {
            connection = connectionCreator.createUrlConnection(request)
            connection.connect()
            return responseManager.handleResponse(connection, request.responseCacheKeys, typeOfT)

        } catch (e: Exception) {
            val message = "Request Error: ${e.localizedMessage ?: e.message}"
            Logger.log(WARN) { message }
            return Response.Error(
                AdaptyError(
                    originalError = e,
                    message = message,
                    adaptyErrorCode = AdaptyErrorCode.REQUEST_FAILED
                )
            )
        } finally {
            connection?.disconnect()
        }
    }

}