package me.devoxin.jukebot.events

import io.sentry.Sentry
import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.SubCommandFunction
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.exceptions.BadArgument
import me.devoxin.flight.api.hooks.DefaultCommandEventAdapter
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.annotations.Checks.DJ
import me.devoxin.jukebot.annotations.Checks.Playing
import me.devoxin.jukebot.annotations.Checks.Premium
import me.devoxin.jukebot.annotations.Checks.PremiumServer
import me.devoxin.jukebot.annotations.Prerequisites.RequireMutualVoiceChannel
import me.devoxin.jukebot.annotations.Prerequisites.TriggerConnect
import me.devoxin.jukebot.extensions.audioPlayer
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.isDJ
import me.devoxin.jukebot.extensions.premiumTier
import me.devoxin.jukebot.utils.Constants
import net.dv8tion.jda.api.Permission
import org.slf4j.LoggerFactory
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class FlightEventAdapter : DefaultCommandEventAdapter() {
    override fun onBadArgument(ctx: Context, command: CommandFunction, error: BadArgument) {
        val message = error.original?.localizedMessage ?: error.localizedMessage
        val syntax = buildString {
            append("/${command.name}")

            if (ctx.invokedCommand is SubCommandFunction) {
                append(" ")
                append(ctx.invokedCommand.name)
            }

            for (arg in ctx.invokedCommand.arguments) {
                append(" ")
                append(arg.format(withType = false))
            }
        }

        ctx.embed("Incorrect Command Usage", """
            $message
            
            `${error.argument.slashFriendlyName}`': ${error.argument.description}
            
            Syntax: `$syntax`
            
            If you're still having trouble using this command, join the [support server](${Constants.HOME_SERVER})
        """.trimIndent())
    }

    override fun onBotMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>) {
    }

    override fun onUserMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>) {
    }

    override fun onCommandError(ctx: Context, command: CommandFunction, error: Throwable) {
        log.error("command ${command.name} encountered an error during execution!", error)
        Sentry.capture(error)

        ctx.runCatching {
            embed(
                "Command Error",
                "An error occurred during command execution.\nWe're sorry for any inconvenience caused.\nIf this error persists, please join our [support server](${Constants.HOME_SERVER})"
            )
        }
    }

    override fun onCommandPreInvoke(ctx: Context, command: CommandFunction): Boolean {
        val executed = ctx.invokedCommand // this can be CommandFunction or SubCommandFunction
        val method = executed.method // we want to check the annotations on the command being invoked

        method.findAnnotation<DJ>()?.let {
            if (!ctx.isDJ(it.alone)) {
                ctx.embed("Not a DJ", "You need to be a DJ to use this command.")
                return false
            }
        }

        method.findAnnotation<Playing>()?.let {
            if (ctx.guild?.audioManager?.isConnected != true || ctx.audioPlayer?.isPlaying != true) {
                ctx.embed("Not Playing", "Nothing is currently playing.")
                return false
            }
        }

        if (!Launcher.isSelfHosted) {
            method.findAnnotation<Premium>()?.let {
                val requiredTier = it.tier

                if (requiredTier > ctx.premiumTier) {
                    ctx.embed("Premium Required", "You must have [premium tier $requiredTier or higher](https://patreon.com/devoxin)")
                    return false
                }
            }

            method.findAnnotation<PremiumServer>()?.let {
                if (!ctx.isFromGuild) {
                    return@let
                }

                if (!Database.getIsPremiumServer(ctx.guild!!.idLong)) {
                    ctx.embed("Premium Server Required", "This server is not a premium server.\n[Join Premium to enable premium servers!](https://patreon.com/devoxin)")
                    return false
                }
            }
        }

        return when {
            method.hasAnnotation<RequireMutualVoiceChannel>() && !checkVoiceState(ctx, true) -> false
            method.hasAnnotation<TriggerConnect>() && !connectToChannel(ctx) -> false
            else -> true
        }
    }

    override fun onInternalError(error: Throwable) {
        log.error("internal error occurred within flight", error)
        Sentry.capture(error)
    }

    override fun onParseError(ctx: Context, command: CommandFunction, error: Throwable) {
        log.error("parser error in command ${command.name}", error)
    }

    private fun checkVoiceState(ctx: Context, requireConnection: Boolean): Boolean {
        val audioManager = ctx.guild?.audioManager
        val memberVoice = ctx.member?.voiceState
        val isConnected = audioManager?.connectedChannel != null
        val channelName = audioManager?.connectedChannel?.name

        if (memberVoice?.channel == null) {
            return if (isConnected) {
                ctx.embed("Voice Channel", "You need to join **$channelName**!")
                false
            } else {
                ctx.embed("Voice Channel", "You need to join a voice channel!")
                false
            }
        }

        if (isConnected && memberVoice.channel?.idLong != audioManager?.connectedChannel?.idLong) {
            ctx.embed("Voice Channel", "You need to join **$channelName**!")
            return false
        }

        if (requireConnection && !isConnected) {
            ctx.embed("Not Connected", "I'm not connected to a voice channel.")
            return false
        }

        return true
    }

    private fun connectToChannel(ctx: Context): Boolean {
        if (!checkVoiceState(ctx, false)) {
            return false
        }

        val audioManager = ctx.guild?.audioManager ?: return false
        val connectedChannel = audioManager.connectedChannel
        val memberVoice = ctx.member?.voiceState

        if (connectedChannel == null) {
            val audioChannel = memberVoice?.channel

            when {
                audioChannel == null -> {
                    ctx.embed("Voice Channel", "You need to join a voice channel!")
                    return false
                }
                !audioChannel.guild.selfMember.hasPermission(audioChannel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK) -> {
                    ctx.embed(
                        "Unable to Connect",
                        "The permissions of **${audioChannel.name}** prevent me from connecting.\nCheck that I have the `Connect` and `Speak` permissions."
                    )
                    return false
                }
                audioChannel.userLimit > 0 && audioChannel.members.size >= audioChannel.userLimit && !audioChannel.guild.selfMember.hasPermission(audioChannel, Permission.VOICE_MOVE_OTHERS) -> {
                    ctx.embed("Unable to Connect", "Your voice channel is currently full.\nRaise the user limit, or move to another channel.")
                    return false
                }
            }
        } else {
            if (memberVoice?.channel != connectedChannel) {
                ctx.embed("Voice Channel", "You need to join **${connectedChannel.name}**!")
                return false
            }
        }

        return true
    }

    companion object {
        private val log = LoggerFactory.getLogger(FlightEventAdapter::class.java)
    }
}
