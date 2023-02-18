package me.devoxin.jukebot.framework

import me.devoxin.jukebot.utils.capitalise

enum class CommandCategory(val description: String) {
    PLAYBACK("Commands that allow you to find and play songs."),
    CONTROLS("Commands that modify aspects of the player."),
    QUEUE("Commands that allow you to manage the queue."),
    MISC("Commands that don't fit in the other categories.");

    fun toTitleCase() = this.toString().lowercase().capitalise()
}
