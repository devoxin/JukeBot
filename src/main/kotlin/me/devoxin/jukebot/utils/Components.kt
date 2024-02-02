package me.devoxin.jukebot.utils

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

object Components {
    val nowPlayingRowUnpaused = ActionRow.of(
        Button.secondary("np_prev", Emoji.fromCustom("prev", 1200984412611948605, false)),
        Button.secondary("np_pause", Emoji.fromCustom("pause", 1200984439958798458, false)),
        Button.secondary("np_next", Emoji.fromCustom("next", 1200984449068843099, false))
    )

    val nowPlayingRowPaused = ActionRow.of(
        Button.secondary("np_prev", Emoji.fromCustom("prev", 1200984412611948605, false)),
        Button.secondary("np_pause", Emoji.fromCustom("play", 1200984429527564338, false)),
        Button.secondary("np_next", Emoji.fromCustom("next", 1200984449068843099, false))
    )
}
