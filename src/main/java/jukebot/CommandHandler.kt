package jukebot

import com.google.common.reflect.ClassPath
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import jukebot.utils.Command
import jukebot.utils.Context
import jukebot.utils.Helpers
import jukebot.utils.Permissions
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class CommandHandler : ListenerAdapter() {

    companion object {
        val commands = HashMap<String, Command>()
    }

    val permissions = Permissions()

    init {
        val classes = ClassPath.from(this::class.java.classLoader).getTopLevelClasses("jukebot.commands")

        for (klass in classes) {
            val clazz = klass.load()

            val cmd = clazz.newInstance() as Command

            if (!cmd.properties().enabled || cmd.properties().nsfw && !JukeBot.isNSFWEnabled()) {
                continue
            }

            commands[cmd.name().toLowerCase()] = cmd
        }

        JukeBot.LOG.info("Loaded ${commands.size} commands!")
    }

    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        try {
            if (!e.guild.isAvailable || e.author.isBot || e.author.isFake || !permissions.canSendTo(e.channel) || Database.isBlocked(e.author.idLong))
                return

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
                    .filter { c -> c.key == command || c.value.properties().aliases.contains(command) }
                    .values
                    .firstOrNull()

            if (foundCommand == null || foundCommand.properties().developerOnly && JukeBot.botOwnerId != e.author.idLong)
                return

            foundCommand.execute(Context(e, args, guildPrefix))
        } catch (err: Exception) {
            val formatted = "An error occurred in the CommandHandler!\n" +
                    "\tMessage: ${e.message.contentStripped}\n" +
                    "\tBot/Webhook: ${e.author.isBot || e.isWebhookMessage}\n" +
                    "\tCause: $err\n" +
                    "\tStack: ${err.stackTrace.joinToString("\n\t\t")}"

            JukeBot.LOG.error(formatted)

            e.channel.sendMessage(EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("An unknown error occurred!")
                    .setDescription("The error has been logged, we're sorry for any inconvenience caused!")
                    .build()
            ).queue()
        }
    }

    override fun onGuildJoin(e: GuildJoinEvent) {
        val bots = e.guild.members.filter { member -> member.user.isBot }.size
        if (bots / e.guild.members.size > 0.6)
            e.guild.leave().queue()
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
        if (e.member.user.idLong == e.jda.selfUser.idLong && JukeBot.hasPlayer(e.guild.idLong))
            JukeBot.getPlayer(e.guild.audioManager).stop()
    }

}
