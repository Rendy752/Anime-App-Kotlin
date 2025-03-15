package com.example.animeapp.utils

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

object ResponseHandler {
    fun <T> handleCommonResponse(response: Response<T>): Resource<T> {
        if (response.isSuccessful) {
            response.body()?.let { return Resource.Success(it) }
                ?: return Resource.Error("Response body is null")
        }
        return Resource.Error(response.errorBody()?.string() ?: "Unknown error")
    }

    fun <T, R> handleResponse(
        response: Response<T>,
        onSuccess: (T) -> R?,
        onError: ((String?) -> String?)? = null
    ): Resource<R> {
        if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                val result = onSuccess(responseBody)
                return if (result != null) {
                    Resource.Success(result)
                } else {
                    val errorMessage = onError?.invoke("Failed to process response body")
                        ?: "Failed to process response body"
                    Resource.Error(errorMessage)
                }
            } else {
                val errorMessage =
                    onError?.invoke("Response body is null") ?: "Response body is null"
                return Resource.Error(errorMessage)
            }
        } else {
            val errorMessage = onError?.invoke(response.errorBody()?.string() ?: "Unknown error")
                ?: response.errorBody()?.string() ?: "Unknown error"
            return Resource.Error(errorMessage)
        }
    }

    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Response<T> {
        return try {
            val response = apiCall.invoke()
            if (response.isSuccessful) {
                response
            } else {
                response
            }
        } catch (e: IOException) {
            Response.error(500, "Network error".toResponseBody())
        } catch (e: HttpException) {
            Response.error(e.code(), "HTTP error".toResponseBody())
        } catch (e: Exception) {
            Response.error(500, "Unknown error".toResponseBody())
        }
    }
}