package jukebot.apis.patreon

import org.json.JSONObject

class PatreonUser(
    val firstName: String,
    val lastName: String,
    val email: String,
    val pledgeCents: Int,
    val isDeclined: Boolean,
    val discordId: Long?
) {
    companion object {
        fun fromJsonObject(userObj: JSONObject, pledgeObj: JSONObject): PatreonUser {
            val userAttr = userObj.getJSONObject("attributes")
            val pledgeAttr = pledgeObj.getJSONObject("attributes")

            val connections = userAttr.getJSONObject("social_connections")
            val discordId = connections.optJSONObject("discord")
                ?.getLong("user_id")

            return PatreonUser(
                userAttr.getString("first_name"),
                userAttr.optString("last_name", ""),
                userAttr.getString("email"),
                pledgeAttr.getInt("amount_cents"),
                !pledgeAttr.isNull("declined_since"),
                discordId
            )
        }
    }
}
