package jukebot.apis.patreon

import jukebot.JukeBot
import jukebot.utils.json
import okhttp3.Request
import org.apache.http.client.utils.URIBuilder
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture

class PatreonAPI(private var accessToken: String) {

    fun setAccessToken(token: String) {
        accessToken = token
    }

    fun fetchPledgesOfCampaign(campaignId: String): CompletableFuture<List<PatreonUser>> {
        val future = CompletableFuture<List<PatreonUser>>()

        getPageOfPledge(campaignId) {
            future.complete(it)
        }

        return future
    }

    private fun getPageOfPledge(campaignId: String, offset: String? = null,
                                users: MutableSet<PatreonUser> = mutableSetOf(), cb: (List<PatreonUser>) -> Unit) {
        val url = URIBuilder("$BASE_URL/campaigns/$campaignId/pledges")

        url.addParameter("include", "pledge,patron")

        if (offset != null) {
            url.addParameter("page[cursor]", offset)
        }

        JukeBot.httpClient.request {
            url(url.build().toURL())
            header("Authorization", "Bearer $accessToken")
        }.queue({
            if (!it.isSuccessful) {
                JukeBot.LOG.error("Unable to get list of pledges ({}): {}", it.code(), it.message())
                it.close()

                return@queue cb(users.toList())
            }

            val json = it.json() ?: return@queue cb(users.toList())
            val pledges = json.getJSONArray("data")

            json.getJSONArray("included").forEachIndexed { index, user ->
                val obj = user as JSONObject

                if (obj.getString("type") == "user") {
                    users.add(buildUser(obj, pledges.getJSONObject(index)))
                }
            }

            val nextPage = getNextPage(json) ?: return@queue cb(users.toList())
            getPageOfPledge(campaignId, nextPage, users, cb)
        }, {
            JukeBot.LOG.error("Unable to get list of pledges", it)
            return@queue cb(users.toList())
        })
    }

    private fun getNextPage(json: JSONObject): String? {
        val links = json.getJSONObject("links")

        if (!links.has("next")) {
            return null
        }

        return parseQueryString(links.getString("next"))["page[cursor]"]
//
//        val queryParams = URLEncodedUtils.parse(URI(links.getString("next")), Charset.forName("utf8"))
//        return queryParams.firstOrNull { it.name == "page[cursor]" }?.value
    }

    fun parseQueryString(url: String): HashMap<String, String> {
        val query = URI(url).query
        val pairs = query.split("&")
        val map = hashMapOf<String, String>()

        for (pair in pairs) {
            val nameValue = pair.split("=")
            val key = URLDecoder.decode(nameValue[0], CHARSET)
            val value = URLDecoder.decode(nameValue[1], CHARSET)

            map[key] = value
        }

        return map
    }

    private fun buildUser(user: JSONObject, pledge: JSONObject): PatreonUser {
        val userAttr = user.getJSONObject("attributes")
        val pledgeAttr = pledge.getJSONObject("attributes")

        val connections = userAttr.getJSONObject("social_connections")
        val discordId = if (!connections.isNull("discord")) {
            connections.getJSONObject("discord").getLong("user_id")
        } else {
            null
        }

        return PatreonUser(
            userAttr.getString("first_name"),
            userAttr.getString("last_name"),
            userAttr.getString("email"),
            pledgeAttr.getInt("amount_cents"),
            !pledgeAttr.isNull("declined_since"),
            discordId
        )
    }

    companion object {
        private const val BASE_URL = "https://www.patreon.com/api/oauth2/api"
        private val CHARSET = Charsets.UTF_8
    }
}
