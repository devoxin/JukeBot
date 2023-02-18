package me.devoxin.jukebot.utils

import me.devoxin.jukebot.JukeBot
import java.util.concurrent.TimeUnit

object Limits {
    const val CUSTOM_PLAYLIST_MAX_TRACKS = 100

    fun playlist(tier: Int): Int {
        return when {
            JukeBot.isSelfHosted -> Integer.MAX_VALUE
            tier >= 2            -> Integer.MAX_VALUE
            tier == 1            -> 1000
            else                 -> 100
        }
    }

    fun duration(tier: Int): Long {
        return when {
            tier >= 2 -> Long.MAX_VALUE
            tier == 1 -> TimeUnit.HOURS.toMillis(5)
            else      -> TimeUnit.HOURS.toMillis(2)
        }
    }

    fun customPlaylists(tier: Int): Int {
        return when {
            tier >= 2 -> 100
            tier >= 1 -> 50
            else      -> 5
        }
    }
}
