package jukebot.apis

import jukebot.JukeBot
import jukebot.utils.RequestUtil
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody

open class Api(baseUrl: String) {
    private val baseUrl = HttpUrl.get(baseUrl)

    inner class RequestBuilder {
        private val url = baseUrl.newBuilder()

        fun path(path: String): RequestBuilder {
            url.addPathSegments(path)
            return this
        }

        fun query(k: String, v: String): RequestBuilder {
            url.setQueryParameter(k, v)
            return this
        }

        fun toRequestBuilder(): Request.Builder {
            return Request.Builder()
                .url(url.build())
        }

        fun toRequest(): Request = toRequestBuilder().build()

        fun send() = JukeBot.httpClient.request(toRequest())
    }

    open fun request(req: RequestBuilder.() -> Unit) = RequestBuilder().apply(req).send()
}
