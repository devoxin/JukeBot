package me.devoxin.jukebot.patreon

import com.grack.nanojson.JsonObject

class PatreonUser(
    val firstName: String,
    val lastName: String,
    val email: String,
    val pledgeCents: Int,
    val isDeclined: Boolean,
    val discordId: Long?
) {
    companion object {
        fun fromJsonObject(userObj: JsonObject, pledgeObj: JsonObject): PatreonUser {
            val userAttr = userObj.getObject("attributes")
            val pledgeAttr = pledgeObj.getObject("attributes")

            val connections = userAttr.getObject("social_connections")
            val discordId = connections.getObject("discord")
                ?.getString("user_id")?.toLong()

            return PatreonUser(
                userAttr.getString("first_name"),
                userAttr.getString("last_name", ""),
                userAttr.getString("email"),
                pledgeAttr.getInt("amount_cents"),
                !pledgeAttr.isNull("declined_since"),
                discordId
            )
        }
    }
}
