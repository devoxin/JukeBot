package me.devoxin.jukebot.framework

import net.dv8tion.jda.api.interactions.commands.OptionType

annotation class Option(
    val name: String,
    val description: String,
    val type: OptionType,
    val required: Boolean = true
)
