package me.devoxin.jukebot.integrations.patreon

import org.json.JSONObject

class PatreonUser(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val pledgeCents: Int,
    val isDeclined: Boolean,
    val discordId: Long?/*,
    val tier: PatronTier*/
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PatreonUser) return false

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode()
    }

    companion object {
        fun fromJsonObject(userObj: JSONObject, pledgeObj: JSONObject): PatreonUser {
            val userAttr = userObj.getJSONObject("attributes")
            val pledgeAttr = pledgeObj.getJSONObject("attributes")

            val connections = userAttr.getJSONObject("social_connections")
            val discordId = connections.optJSONObject("discord")
                ?.getLong("user_id")

            //val tierId = pledgeObj.getJSONObject("relationships")
            //    ?.optJSONObject("reward")
            //    ?.optJSONObject("data")
            //    ?.optInt("id")
            //    ?: 0

            return PatreonUser(
                userObj.getInt("id"),
                userAttr.getString("first_name"),
                userAttr.optString("last_name", ""),
                userAttr.getString("email"),
                pledgeAttr.getInt("amount_cents"),
                !pledgeAttr.isNull("declined_since"),
                discordId/*,
                PatronTier.from(tierId)*/
            )
        }
    }
}
