package me.devoxin.jukebot.integrations.patreon

import me.devoxin.jukebot.integrations.patreon.entities.Patron

data class ResultPage(
    val pledges: List<Patron>,
    val offset: String?,
    val hasMore: Boolean = offset != null
)
