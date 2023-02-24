package me.devoxin.jukebot.handlers

import io.sentry.Sentry
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Arguments
import me.devoxin.jukebot.framework.CommandScanner
import me.devoxin.jukebot.framework.Context
import me.devoxin.jukebot.utils.canSendEmbed
import me.devoxin.jukebot.utils.separate
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.LoggerFactory

class CommandHandler : EventListener {
    override fun onEvent(event: GenericEvent) {
        when (event) {
            is MessageReceivedEvent -> if (event.isFromGuild) onGuildMessageReceived(event)
            is SlashCommandInteractionEvent -> if (event.isFromGuild) onSlashCommand(event)
        }
    }

    private fun onGuildMessageReceived(e: MessageReceivedEvent) {
        if (e.author.isBot || e.isWebhookMessage || !e.guildChannel.canSendEmbed() || Database.getIsBlocked(e.author.idLong)) {
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

        val foundCommand = commands[command]
            ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }
            ?: return

        if (foundCommand.properties.developerOnly && JukeBot.botOwnerId != e.author.idLong) {
            return
        }

        val context = Context(
            Arguments.MessageArguments(e.message, args), guildPrefix,
            e.jda, e.author, e.member!!, e.guildChannel, e.guild, e.message, null
        )

        runCatching {
            foundCommand.runChecks(context)
        }.onFailure {
            logger.error("An error occurred during invocation of command ${foundCommand.name}", it)
            Sentry.capture(it)
        }
    }

    private fun onSlashCommand(e: SlashCommandInteractionEvent) {
        if (e.user.isBot || !e.guildChannel.canSendEmbed() || Database.getIsBlocked(e.user.idLong)) {
            return
        }

        val command = commands[e.name]
            ?: return e.reply("Command not found.").setEphemeral(true).queue()

        if (command.properties.developerOnly && JukeBot.botOwnerId != e.user.idLong) {
            return
        }

        val context = Context(
            Arguments.SlashArguments(e), "/",
            e.jda, e.user, e.member!!, e.guildChannel, e.guild!!, null, e
        )

        runCatching {
            command.runChecks(context)
        }.onFailure {
            logger.error("An error occurred during invocation of command ${command.name}", it)
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
