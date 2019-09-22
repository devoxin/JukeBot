package jukebot.utils

import okhttp3.*
import org.slf4j.LoggerFactory
import java.io.IOException

class RequestUtil {
    private val httpClient = OkHttpClient()
    private val logger = LoggerFactory.getLogger(this.javaClass)

    inner class PendingRequest(private val request: Request) {

        fun block(): Response {
            return httpClient.newCall(request).execute()
        }

        fun queue(success: (Response) -> Unit, failure: (IOException) -> Unit) {
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    logger.error("An error occurred during a HTTP request to ${call.request().url()}", e)
                    failure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    success(response)
                }
            })
        }
    }

    fun get(url: String, headers: Headers = Headers.of()): PendingRequest {
        return makeRequest("GET", url, null, headers)
    }

    fun makeRequest(method: String, url: String, body: RequestBody? = null, headers: Headers): PendingRequest {
        val request = Request.Builder()
            .method(method.toUpperCase(), body)
            .header("User-Agent", "JukeBot (https://github.com/Devoxin/JukeBot)")
            .headers(headers)
            .url(url)

        return PendingRequest(request.build())
    }

    fun makeRequest(request: Request): PendingRequest {
        return PendingRequest(request)
    }
}
