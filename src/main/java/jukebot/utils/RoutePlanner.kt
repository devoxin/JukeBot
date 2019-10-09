package jukebot.utils

import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.conn.routing.HttpRoutePlanner
import org.apache.http.protocol.HttpContext

class RoutePlanner : HttpRoutePlanner {

    override fun determineRoute(target: HttpHost, request: HttpRequest, context: HttpContext): HttpRoute {
        println(request.requestLine.uri)
        request.params.setParameter("has_verified", 1)
        println(request.requestLine.uri)
        return HttpRoute(target)
    }

}