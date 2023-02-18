package me.devoxin.jukebot.handlers

import io.sentry.Sentry
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.CommandScanner
import me.devoxin.jukebot.framework.Context
import me.devoxin.jukebot.utils.canSendEmbed
import me.devoxin.jukebot.utils.separate
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.LoggerFactory

class CommandHandler : EventListener {
    override fun onEvent(event: GenericEvent) {
        if (event is MessageReceivedEvent && event.isFromGuild) {
            onGuildMessageReceived(event)
        }
    }

    private fun onGuildMessageReceived(e: MessageReceivedEvent) {
        if (e.author.isBot || e.isWebhookMessage || !e.channel.asGuildMessageChannel().canSendEmbed() || Database.getIsBlocked(e.author.idLong)) {
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
        }.onFailure {
            logger.error("An error occurred during invocation of command ${foundCommand.name}", it)
            Sentry.capture(it)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandHandler::class.java)
        private val MENTION_FORMATS by lazy { listOf("<@${JukeBot.selfId}>", "<@!${JukeBot.selfId}>") }

        val commands = CommandScanner("me.devoxin.jukebot.commands").scan().toMutableMap().also {
            logger.info("${it.size} commands in registry")
        }
    }
}
