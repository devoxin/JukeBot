package jukebot.framework

import jukebot.utils.toTitleCase

enum class CommandCategory(val description: String) {
    PLAYBACK("Commands that allow you to find and play songs."),
    CONTROLS("Commands that modify aspects of the player."),
    QUEUE("Commands that allow you to manage the queue."),
    MISC("Commands that don't fit in the other categories.");

    fun toTitleCase(): String {
        return this.toString().lowercase().toTitleCase()
        // Imagine calling .toTitleCase() but it doesn't lowercase
        // the rest of the fucking word???????????????????????????
    }
}
