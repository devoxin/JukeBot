package jukebot.utils

import jukebot.JukeBot
import okhttp3.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.CompletableFuture

class RequestUtil {
    private val httpClient = OkHttpClient()
    private val logger = LoggerFactory.getLogger(this.javaClass)

    inner class PendingRequest(private val request: Request) {
        fun submit(): CompletableFuture<Response> {
            val fut = CompletableFuture<Response>()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    logger.error("An error occurred during a HTTP request to ${call.request().url()}", e)
                    fut.completeExceptionally(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    JukeBot.LOG.debug("Response: code=${response.code()} message=${response.message()}")
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

    fun get(url: String, headers: Headers = Headers.of()): PendingRequest {
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
}
