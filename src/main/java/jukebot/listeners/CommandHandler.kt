package jukebot.listeners

import com.google.common.reflect.ClassPath
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import jukebot.Database
import jukebot.JukeBot
import jukebot.framework.*
import jukebot.utils.Helpers
import jukebot.utils.separate
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class CommandHandler : ListenerAdapter() {

    init {
        val classes = ClassPath.from(this::class.java.classLoader).getTopLevelClasses("jukebot.commands")

        for (klass in classes) {
            val clazz = klass.load()
            var cmd: Command

            try {
                cmd = clazz.getDeclaredConstructor().newInstance() as Command
            } catch (e: Exception) {
                if (e.cause != null && e.cause!!::class.java.isAssignableFrom(CommandInitializationError::class.java)) {
                    continue
                }

                JukeBot.LOG.warn("Command ${clazz.simpleName} failed to load", e)
                continue
            }

            if (!cmd.properties().enabled || cmd.properties().nsfw && !JukeBot.config.nsfwEnabled) {
                continue
            }

            val methods = clazz.methods.filter { it.isAnnotationPresent(SubCommand::class.java) }

            for (meth in methods) {
                val annotation = meth.getAnnotation(SubCommand::class.java)
                val trigger = annotation.trigger.toLowerCase()
                val description = annotation.description

                val wrapper = MethodWrapper(description, meth, cmd)
                cmd.subcommands[trigger] = wrapper
            }

            commands[cmd.name().toLowerCase()] = cmd
        }

        JukeBot.LOG.info("Loaded ${commands.size} commands!")
    }

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        if (!e.guild.isAvailable || e.author.isBot || e.author.isFake || !Helpers.canSendTo(e.channel)
                || Database.getIsBlocked(e.author.idLong)) {
            return
        }

        val guildPrefix = Database.getPrefix(e.guild.idLong)
        val wasMentioned = e.message.contentRaw.startsWith(e.guild.selfMember.asMention)
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
                ?: commands.values.firstOrNull { it.properties().aliases.contains(command) }
                ?: return

        if (foundCommand.properties().developerOnly && JukeBot.botOwnerId != e.author.idLong) {
            return
        }

        foundCommand.runChecks(Context(e, args, originalArgs, guildPrefix))
    }

    companion object {
        val commands = HashMap<String, Command>()
    }

}
