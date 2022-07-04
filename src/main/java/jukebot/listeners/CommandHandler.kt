package jukebot.listeners

import io.sentry.Sentry
import jukebot.Database
import jukebot.JukeBot
import jukebot.framework.CommandScanner
import jukebot.framework.Context
import jukebot.utils.canSendEmbed
import jukebot.utils.separate
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener

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
        if (e.author.isBot || e.isWebhookMessage || !e.channel.canSendEmbed()
            || Database.getIsBlocked(e.author.idLong)
        ) {
            return
        }

        val guildPrefix = Database.getPrefix(e.guild.idLong)
        val mentionTrigger = MENTION_FORMATS.firstOrNull(e.message.contentRaw::startsWith)
        val triggerLength = mentionTrigger?.let { it.length + 1 } ?: guildPrefix.length

        if (!e.message.contentRaw.startsWith(guildPrefix) && (mentionTrigger == null || !e.message.contentRaw.contains(' '))) {
            return
        }

        val content = e.message.contentRaw.substring(triggerLength).trim()
        val (cmdStr, args) = content.split("\\s+".toRegex()).separate()
        val command = cmdStr.lowercase()
        val originalArgs = if (content.length >= command.length) content.substring(command.length).trim() else ""

        val foundCommand = commands[command]
            ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }
            ?: return

        if (foundCommand.properties.developerOnly && JukeBot.botOwnerId != e.author.idLong) {
            return
        }

        runCatching {
            foundCommand.runChecks(Context(e, args, originalArgs, guildPrefix))
        }.onFailure(Sentry::capture)
    }

    companion object {
        private val MENTION_FORMATS by lazy { listOf("<@${JukeBot.selfId}>", "<@!${JukeBot.selfId}>") }
        val commands = CommandScanner("jukebot.commands").scan().toMutableMap()
    }
}
