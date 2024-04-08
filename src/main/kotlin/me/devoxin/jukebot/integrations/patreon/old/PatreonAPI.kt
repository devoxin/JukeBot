//package me.devoxin.jukebot.integrations.patreon.old
//
//import io.sentry.Sentry
//import me.devoxin.jukebot.Database
//import me.devoxin.jukebot.integrations.patreon.ResultPage
//import me.devoxin.jukebot.integrations.patreon.entities.Patron
//import me.devoxin.jukebot.utils.Helpers
//import me.devoxin.jukebot.utils.HttpClient
//import me.devoxin.jukebot.utils.HttpClient.Companion.httpClient
//import okhttp3.HttpUrl
//import okhttp3.HttpUrl.Companion.toHttpUrl
//import org.json.JSONObject
//import org.slf4j.LoggerFactory
//import java.net.URI
//import java.net.URLDecoder
//import java.util.concurrent.CompletableFuture
//import java.util.concurrent.Executors
//import java.util.concurrent.TimeUnit
//
//class PatreonAPI(private val accessToken: String) {
//    private val monitor = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Pledge-Monitor") }
//
//    init {
//        monitor.scheduleAtFixedRate(::checkPledges, 0, 1, TimeUnit.DAYS)
//    }
//
//    private fun checkPledges() {
//        log.info("checking pledges...")
//
//        fetchPledgesOfCampaign("").thenAccept { users ->
//            if (users.isEmpty()) {
//                return@thenAccept log.warn("scheduled pledge clean failed (no users to check)")
//            }
//
//            for (id in Database.getDonorIds()) {
//                val pledge = users.firstOrNull { it.discordId != null && it.discordId == id }
//
//                if (pledge == null || pledge.isDeclined) {
//                    Database.setTier(id, 0)
//                    Database.removePremiumServersOf(id)
//                    log.info("removed $id from donors")
//                    continue
//                }
//
//                val amount = pledge.pledgeCents.toDouble() / 100
//                val friendly = String.format("%1$,.2f", amount)
//                val tier = Database.getTier(id)
//                val calculatedTier = Helpers.calculateTier(amount)
//
//                if (tier != calculatedTier) {
//                    if (calculatedTier < tier) {
//                        val calculatedServerQuota = if (calculatedTier < 3) 0 else ((calculatedTier - 3) / 1) + 1
//                        val allServers = Database.getPremiumServersOf(id)
//
//                        if (allServers.size > calculatedServerQuota) {
//                            log.info("removing some of $id's premium servers to meet quota (quota: $calculatedServerQuota, servers: ${allServers.size}")
//                            val exceededQuotaBy = allServers.size - calculatedServerQuota
//                            (0..exceededQuotaBy).onEach { allServers[it].remove() }
//                        }
//                    }
//                    log.info("adjusting $id's tier (saved: $tier, calculated: $calculatedTier, pledge: $$friendly)")
//                    Database.setTier(id, calculatedTier)
//                }
//            }
//        }.exceptionally {
//            Sentry.capture(it)
//            return@exceptionally null
//        }
//    }
//    private fun fetchPledgesOfCampaign0(campaignId: String, offset: String? = null): CompletableFuture<List<Patron>> {
//        val initialUrl = baseUrl.newBuilder().apply {
//            addPathSegments("api/oauth2/v2/campaigns/$campaignId/members")
//            setQueryParameter("include", "currently_entitled_tiers,user")
//            setQueryParameter("fields[member]", "full_name,last_charge_date,last_charge_status,lifetime_support_cents,currently_entitled_amount_cents,patron_status,pledge_relationship_start")
//            setQueryParameter("fields[user]", "social_connections")
//            setQueryParameter("page[count]", "100")
//        }.build()
//
//        return fetchPageOfPledgeRecursive(initialUrl, mutableListOf())
////            .thenCompose {
////                users.addAll(it.pledges)
////
////                if (it.hasMore && offset != it.offset) {
////                    fetchPledgesOfCampaign0(campaignId, it.offset)
////                } else {
////                    CompletableFuture.completedFuture(emptyList())
////                }
////            }
////            .thenAccept { users.addAll(it) }
////            .thenApply { users }
//    }
//
//    private fun fetchPageOfPledgeRecursive(url: HttpUrl, cache: MutableList<PatreonUser>): CompletableFuture<List<Patron>> {
//        return request { url(url) }.thenApply {
//            val nextLink = getNextPage(it)
//            val members = it.getJSONArray("data")
//            val users = it.getJSONArray("included")
//            val patrons = mutableListOf<Patron>()
//
//            for (user in users) {
//                val obj = user as JSONObject
//
//                if (obj.getString("type") != "user") {
//                    continue
//                }
//
//                val userId = obj.getString("id")
//                val member = members.firstOrNull { m ->
//                    val mObj = m as JSONObject
//                    val userData = mObj.getJSONObject("relationships").getJSONObject("user").getJSONObject("data")
//                    return@firstOrNull userData.getString("id") == userId
//                }
//
//                if (member != null) {
//                    patrons.add(Patron.from(member as JSONObject, obj))
//                }
//            }
//
//            cache.addAll(patrons)
//            nextLink
//        }.thenCompose {
//            when {
//                it != null -> fetchPageOfPledgeRecursive(HttpUrl.get(it), cache)
//                else -> CompletableFuture.completedFuture(cache)
//            }
//        }
//    }
//
//    private fun fetchPageOfPledge(campaignId: String, offset: String?): CompletableFuture<ResultPage> {
//        return get {
//            addPathSegments("api/campaigns/$campaignId/pledges")
//            setQueryParameter("include", "pledge,patron")
//            offset?.let { setQueryParameter("page[cursor]", it) }
//        }.thenApply {
//            val pledges = it.getJSONArray("data")
//            val nextPage = getNextPage(it)
//            val users = mutableListOf<PatreonUser>()
//
//            for ((index, obj) in it.getJSONArray("included").withIndex()) {
//                obj as JSONObject
//
//                if (obj.getString("type") == "user") {
//                    val pledge = pledges.getJSONObject(index)
//                    users.add(PatreonUser.fromJsonObject(obj, pledge))
//                }
//            }
//
//            // users
//            ResultPage(listOf(), nextPage)
//        }
//    }
//
//    private fun getNextPage(json: JSONObject): String? {
//        if (json.isNull("links")) {
//            return null
//        }
//
//        return json.getJSONObject("links")
//            .takeIf { it.has("next") }
//            ?.getString("next")
//        //?.let { parseQueryString(it.getString("next"))["page[cursor]"] }
//    }
//
//    private fun parseQueryString(url: String): Map<String, String> {
//        return URI(url).query
//            .split('&')
//            .map { it.split("=") }
//            .associateBy({ decode(it[0]) }, { decode(it[1]) })
//    }
//
//    private fun decode(s: String) = URLDecoder.decode(s, Charsets.UTF_8)
//
//    private fun get(urlOpts: HttpUrl.Builder.() -> Unit): CompletableFuture<JSONObject> {
//        if (accessToken?.isNotEmpty() != true) {
//            return CompletableFuture.failedFuture(IllegalStateException("Access token is empty!"))
//        }
//
//        val url = baseUrl.newBuilder().apply(urlOpts).build()
//        return request { url(url) }
//    }
//
//    private fun request(urlOpts: HttpUrl.Builder.() -> Unit): HttpClient.PendingRequest {
//        return httpClient.request {
//            url(baseUrl.newBuilder().apply(urlOpts).build())
//            header("Authorization", "Bearer $accessToken")
//        }
//    }
//
//    companion object {
//        private val log = LoggerFactory.getLogger(PatreonAPI::class.java)
//        private val baseUrl = "https://www.patreon.com/".toHttpUrl()
//    }
//
//    // old stuff
//
////    fun fetchPledgesOfCampaign(campaignId: String): CompletableFuture<List<PatreonUser>> {
////        val future = CompletableFuture<List<PatreonUser>>()
////        getPageOfPledge(campaignId, cb = future::complete)
////        return future
////    }
////
////    private fun getPageOfPledge(
////        campaignId: String, offset: String? = null,
////        users: MutableSet<PatreonUser> = mutableSetOf(), cb: (List<PatreonUser>) -> Unit
////    ) {
////        request {
////            addPathSegments("campaigns/$campaignId/pledges")
////            setQueryParameter("include", "pledge,patron")
////            offset?.let { setQueryParameter("page[cursor]", it) }
////        }.queue({
////            if (!it.isSuccessful) {
////                log.error("unable to get list of pledges ({}): {}", it.code, it.body?.string())
////                it.close()
////
////                return@queue cb(users.toList())
////            }
////
////            val json = it.takeIf { it.isSuccessful }?.body?.use { body -> JsonParser.`object`().from(body.byteStream()) }
////                ?: return@queue cb(users.toList())
////
////            val pledges = json.getArray("data")
////
////            json.getArray("included").forEachIndexed { index, user ->
////                val obj = user as JsonObject
////
////                if (obj.getString("type") == "user") {
////                    val pledge = pledges.getObject(index)
////                    users.add(PatreonUser.fromJsonObject(obj, pledge))
////                }
////            }
////
////            val nextPage = getNextPage(json) ?: return@queue cb(users.toList())
////            getPageOfPledge(campaignId, nextPage, users, cb)
////        }, {
////            log.error("unable to get list of pledges", it)
////            return@queue cb(users.toList())
////        })
////    }
////
////    private fun getNextPage(json: JsonObject): String? {
////        val links = json.getObject("links")
////
////        if (!links.has("next")) {
////            return null
////        }
////
////        return parseQueryString(links.getString("next"))["page[cursor]"]
////    }
////
////    private fun parseQueryString(url: String): Map<String, String> {
////        return URI(url).query
////            .split("&")
////            .map { it.split("=") }
////            .associateBy({ decode(it[0]) }, { decode(it[1]) })
////    }
////
////    private fun decode(s: String) = URLDecoder.decode(s, Charsets.UTF_8)
////
////    private fun request(urlOpts: HttpUrl.Builder.() -> Unit): HttpClient.PendingRequest {
////        return httpClient.request {
////            url(baseUrl.newBuilder().apply(urlOpts).build())
////            header("Authorization", "Bearer $accessToken")
////        }
////    }
////
////    companion object {
////        private val log = LoggerFactory.getLogger(PatreonAPI::class.java)
////        private val baseUrl = "https://www.patreon.com/api/oauth2/api".toHttpUrl()
////    }
//}
