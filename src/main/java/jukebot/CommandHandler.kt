package jukebot

import com.google.common.reflect.ClassPath
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import jukebot.commands.Feedback
import jukebot.utils.*
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
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

            commands[cmd.name().toLowerCase()] = cmd
        }

        JukeBot.LOG.info("Loaded ${commands.size} commands!")
    }

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        if (!e.guild.isAvailable || e.author.isBot || e.author.isFake || !Helpers.canSendTo(e.channel)
                || Database.isBlocked(e.author.idLong)) {
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
        val command = content.split("\\s+".toRegex())[0].toLowerCase()
        val args = if (content.length >= command.length) content.substring(command.length).trim() else ""

        val foundCommand = commands
                .filter { it.key == command || it.value.properties().aliases.contains(command) }
                .values
                .firstOrNull() ?: return

        if (foundCommand.properties().developerOnly && JukeBot.botOwnerId != e.author.idLong) {
            return
        }

        foundCommand.runChecks(Context(e, args, guildPrefix))
    }

    override fun onGuildJoin(e: GuildJoinEvent) {
        val bots = e.guild.members.filter { it.user.isBot }.size

        if (bots / e.guild.members.size > 0.6) {
            e.guild.leave().queue()
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        JukeBot.removePlayer(event.guild.idLong)
    }

    override fun onReady(e: ReadyEvent) {
        if (!JukeBot.isReady) {
            e.jda.asBot().applicationInfo.queue { info ->
                JukeBot.botOwnerId = info.owner.idLong
                JukeBot.isSelfHosted = info.idLong != 249303797371895820L && info.idLong != 314145804807962634L

                if (JukeBot.isSelfHosted) {
                    commands.remove("patreon")
                    commands.remove("verify")
                    (commands.remove("feedback") as Feedback).shutdown()
                } else {
                    Helpers.monitor.scheduleAtFixedRate({ Helpers.monitorPledges() }, 0, 1, TimeUnit.DAYS)
                }

                if (info.idLong == 314145804807962634L || JukeBot.isSelfHosted)
                    JukeBot.playerManager.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.HIGH

                JukeBot.isReady = true
            }
        }
    }

    override fun onGuildVoiceLeave(e: GuildVoiceLeaveEvent) {
        if (!e.member.user.isBot) {
            handleLeave(e.channelLeft)
        }
    }

    fun handleLeave(channel: VoiceChannel) {
        val connectedChannel = channel.guild.audioManager.connectedChannel ?: return

        if (!JukeBot.hasPlayer(channel.guild.idLong)) {
            return
        }

        val listeners = connectedChannel.members.filter { !it.user.isBot }.size

        val player = JukeBot.getPlayer(channel.guild.audioManager)

        if (listeners == 0) {
            player.cleanup()
            connectedChannel.guild.audioManager.closeAudioConnection()
        }
    }

    companion object {
        val commands = HashMap<String, Command>()
    }

}
