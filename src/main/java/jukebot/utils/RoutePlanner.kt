package jukebot.utils

import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.conn.routing.HttpRoutePlanner
import org.apache.http.protocol.HttpContext

class RoutePlanner : HttpRoutePlanner {

    override fun determineRoute(target: HttpHost, request: HttpRequest, context: HttpContext): HttpRoute {
        println(request.requestLine.uri)
        println("headers: ${request.allHeaders.map { "${it.name}: ${it.value}" }}")
        return HttpRoute(target)
    }

}