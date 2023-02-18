package me.devoxin.jukebot.framework

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.utils.Constants
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel

abstract class Command(private val executionType: ExecutionType) {
    val subcommands = hashMapOf<String, MethodWrapper>()

    /**
     * @return True, if execution of the invoker can proceed.
     */
    open fun checkVoiceState(context: Context, requireConnection: Boolean): Boolean {
        val audioManager = context.guild.audioManager
        val isConnected = audioManager.connectedChannel != null
        val memberVoice = context.member.voiceState!!
        val channelName = audioManager.connectedChannel?.name

        if (memberVoice.channel == null) {
            return if (isConnected) {
                context.embed("No Mutual VoiceChannel", "You need to join **$channelName**!")
                false
            } else {
                context.embed("No VoiceChannel", "You need to join a VoiceChannel!")
                false
            }
        }

        if (isConnected && memberVoice.channel?.idLong != audioManager.connectedChannel?.idLong) {
            context.embed("No Mutual VoiceChannel", "You need to join **$channelName**!")
            return false
        }

        // requireConnection => Should check if bot's in a voicechannel?

        return true
    }

    /**
     * @return True, if execution of the invoker can proceed.
     */
    open fun connectToChannel(context: Context): Boolean {
        if (!checkVoiceState(context, false)) {
            return false
        }

        val audioManager = context.guild.audioManager
        val isConnected = audioManager.connectedChannel != null
        val memberVoice = context.member.voiceState!!

        if (!isConnected) {
            val audioChannel = memberVoice.channel!!

            if (!audioChannel.guild.selfMember.hasPermission(audioChannel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)) {
                context.embed(
                    "Unable to Connect",
                    "The VoiceChannel permissions prevent me from connecting.\n" +
                        "Check that I have the `Connect` and `Speak` permissions."
                )
                return false
            }

            val userLimit = (audioChannel as? VoiceChannel)?.userLimit ?: 0
            if (userLimit > 0 && audioChannel.members.size >= userLimit && !audioChannel.guild.selfMember.hasPermission(audioChannel, Permission.VOICE_MOVE_OTHERS)) {
                context.embed(
                    "Unable to Connect",
                    "Your VoiceChannel is currently full.\n" +
                        "Raise the user limit, or move to another channel."
                )
                return false
            }

            audioManager.openAudioConnection(audioChannel)
        }

        return true
    }

    open fun runCommandPreChecks(context: Context): Boolean {
        check(CommandChecks.Dj::class.java)?.let {
            if (!context.isDJ(it.alone)) {
                context.embed(
                    "Not a DJ",
                    "You need to be a DJ to use this command.\n[See here on how to become a DJ](${Constants.WEBSITE}/faq)"
                )
                return false
            }
        }

        check(CommandChecks.Playing::class.java)?.let {
            if (!context.guild.audioManager.isConnected || !context.audioPlayer.isPlaying) {
                context.embed("Not Playing", "Nothing is currently playing.")
                return false
            }
        }

        if (!JukeBot.isSelfHosted) {
            check(CommandChecks.Donor::class.java)?.let {
                val requiredTier = this.javaClass.getAnnotation(CommandChecks.Donor::class.java).tier
                if (requiredTier > context.donorTier) {
                    context.embed("Command Unavailable", "You must be a [donor in Tier $requiredTier or higher](https://patreon.com/devoxin)")
                    return false
                }
            }
        }

        return true
    }

    open fun runChecks(context: Context) {
        if (!runCommandPreChecks(context)) {
            return
        }

        if (executionType == ExecutionType.REQUIRE_MUTUAL &&
            !checkVoiceState(context, true)
        ) {
            return
        } else if (executionType == ExecutionType.TRIGGER_CONNECT) {
            if (context.args.isEmpty() && context.message.attachments.size == 0) {
                return context.embed(name, "You need to specify an identifier to lookup.")
            }

            if (!connectToChannel(context)) {
                return
            }
        }

        execute(context)
    }

    private fun <T : Annotation> check(klass: Class<T>): T? {
        return if (this.javaClass.isAnnotationPresent(klass)) {
            this.javaClass.getAnnotation(klass)
        } else {
            null
        }
    }

    open fun destroy() {}

    abstract fun execute(context: Context)

    val properties: CommandProperties
        get() = this.javaClass.getAnnotation(CommandProperties::class.java)

    val name: String
        get() = this.javaClass.simpleName

    enum class ExecutionType {
        TRIGGER_CONNECT,
        REQUIRE_MUTUAL,
        STANDARD
    }
}
