package jukebot.utils

import jukebot.JukeBot
import java.util.concurrent.TimeUnit

object Limits {
    fun playlist(tier: Int): Int {
        return when {
            JukeBot.isSelfHosted -> Integer.MAX_VALUE
            tier >= 2 -> Integer.MAX_VALUE
            tier == 1 -> 1000
            else -> 100
        }
    }

    fun duration(tier: Int): Long {
        return when {
            tier >= 2 -> Long.MAX_VALUE
            tier == 1 -> TimeUnit.HOURS.toMillis(5)
            else -> TimeUnit.HOURS.toMillis(2)
        }
    }

    fun customPlaylists(tier: Int): Int {
        return when {
            tier >= 2 -> 100
            tier >= 1 -> 50
            else -> 5
        }
    }
}
