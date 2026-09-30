package com.zekid.contactnotifier.data.network

import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class NtfyPublishRequest(
    val topic: String,
    val message: String,
    val title: String? = null,
    val priority: Int? = null,
    val tags: List<String>? = null
)

interface NtfyApi {
    /**
     * JSON publish (POST server root). Title/message/tags go in the JSON
     * body as UTF-8, so Korean titles work. OkHttp rejects non-ASCII
     * characters in HTTP headers, which is why the old Title header
     * broke every send.
     */
    @POST
    suspend fun publish(
        @Url url: String,
        @Body request: NtfyPublishRequest
    ): Response<ResponseBody>
}
