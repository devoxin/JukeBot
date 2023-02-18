package me.devoxin.jukebot.framework

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandProperties(
    val aliases: Array<String> = [],
    val description: String = "No description available",
    val category: CommandCategory = CommandCategory.MISC,
    val developerOnly: Boolean = false,
    val nsfw: Boolean = false,
    val enabled: Boolean = true
)