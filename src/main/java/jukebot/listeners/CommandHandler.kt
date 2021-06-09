package jukebot.listeners

import io.sentry.Sentry
import jukebot.Database
import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandScanner
import jukebot.framework.Context
import jukebot.utils.Helpers
import jukebot.utils.separate
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandHandler : EventListener {
    init {
        JukeBot.log.info("${commands.size} commands in registry")
    }

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReceivedEvent) {
            onGuildMessageReceived(event)
        }
    }

    private fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        if (e.author.isBot || e.isWebhookMessage || !Helpers.canSendTo(e.channel)
            || Database.getIsBlocked(e.author.idLong)) {
            return
        }

        val guildPrefix = Database.getPrefix(e.guild.idLong)
        val wasMentioned = e.message.contentRaw.startsWith("<@${e.guild.selfMember.id}>")
            || e.message.contentRaw.startsWith("<@!${e.guild.selfMember.id}>")
        val triggerLength = if (wasMentioned) e.guild.selfMember.asMention.length + 1 else guildPrefix.length

        if (!e.message.contentRaw.startsWith(guildPrefix) && !wasMentioned)
            return

        if (wasMentioned && !e.message.contentRaw.contains(" "))
            return

        val content = e.message.contentRaw.substring(triggerLength).trim()
        val (cmdStr, args) = content.split("\\s+".toRegex()).separate()
        val command = cmdStr.toLowerCase()
        val originalArgs = if (content.length >= command.length) content.substring(command.length).trim() else ""

        val foundCommand = commands[command]
            ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }
            ?: return

        if (foundCommand.properties.developerOnly && JukeBot.botOwnerId != e.author.idLong) {
            return
        }

        try {
            foundCommand.runChecks(Context(e, args, originalArgs, guildPrefix))
        } catch (e: Exception) {
            Sentry.capture(e)
        }
    }

    companion object {
        val commands = CommandScanner("jukebot.commands").scan().toMutableMap()
    }
}
