package me.devoxin.jukebot.utils

import okhttp3.*
import okhttp3.Headers.Companion.headersOf
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.CompletableFuture

class HttpClient {
    private val httpClient = OkHttpClient()

    inner class PendingRequest(private val request: Request) {
        fun submit(): CompletableFuture<Response> {
            val fut = CompletableFuture<Response>()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    log.error("an error occurred during a http request to ${call.request().url}", e)
                    fut.completeExceptionally(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    log.debug("response: code=${response.code} message=${response.message}")
                    fut.complete(response)
                }
            })

            return fut
        }

        fun queue(success: (Response) -> Unit, failure: (IOException) -> Unit) {
            submit()
                .thenAccept(success)
                .exceptionally {
                    failure(it as IOException)
                    null
                }
        }
    }

    fun get(url: String, headers: Headers = headersOf()): PendingRequest {
        return request {
            get()
            url(url)
            headers(headers)
        }
    }

    fun request(opts: Request.Builder.() -> Unit): PendingRequest {
        val req = Request.Builder()
            .header("User-Agent", "JukeBot (https://github.com/Devoxin/JukeBot)")
            .apply(opts)
            .build()

        return PendingRequest(req)
    }

    fun request(request: Request) = PendingRequest(request)

    companion object {
        private val log = LoggerFactory.getLogger(HttpClient::class.java)

        val httpClient = HttpClient()
    }
}
