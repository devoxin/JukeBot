package jukebot.framework

import jukebot.JukeBot
import net.dv8tion.jda.api.Permission

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
            val voiceChannel = memberVoice.channel!!

            if (!voiceChannel.guild.selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)) {
                context.embed(
                    "Unable to Connect",
                    "The VoiceChannel permissions prevent me from connecting.\n" +
                        "Check that I have the `Connect` and `Speak` permissions."
                )
                return false
            }

            if (voiceChannel.userLimit > 0 && voiceChannel.members.size >= voiceChannel.userLimit &&
                !voiceChannel.guild.selfMember.hasPermission(Permission.VOICE_MOVE_OTHERS)) {
                context.embed(
                    "Unable to Connect",
                    "Your VoiceChannel is currently full.\n" +
                        "Raise the user limit, or move to another channel."
                )
                return false
            }

            audioManager.openAudioConnection(voiceChannel)
        }

        return true
    }

    open fun runCommandPreChecks(context: Context): Boolean {
        if (!this.javaClass.isAnnotationPresent(CommandCheck::class.java)) {
            return true
        }

        val checks = this.javaClass.getAnnotation(CommandCheck::class.java)

        if (checks.dj != DjCheck.NONE) {
            val allowLone = checks.dj == DjCheck.ROLE_OR_ALONE

            if (!context.isDJ(allowLone)) {
                context.embed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.serux.pro/faq)")
                return false
            }
        }

        if (checks.isPlaying) {
            val ap = context.getAudioPlayer()

            if (!ap.isPlaying) {
                context.embed("Not Playing", "Nothing is currently playing.")
                return false
            }
        }

        if (checks.donor > 0 && !JukeBot.isSelfHosted) {
            if (checks.donor > context.donorTier) {
                context.embed("Spotify Unavailable", "You must be a [donor in Tier 2 or higher](https://patreon.com/devoxin)")
                return false
            }
        }

        return true
    }

    open fun runChecks(context: Context) {
        when (executionType) {
            //ExecutionType.STANDARD -> execute(context)
            ExecutionType.REQUIRE_MUTUAL -> {
                if (!checkVoiceState(context, true)) {
                    return
                }
            }
            ExecutionType.TRIGGER_CONNECT -> {
                if (context.args.isEmpty() && context.message.attachments.size == 0) {
                    return context.embed(name(), "You need to specify an identifier to lookup.")
                }

                if (!connectToChannel(context)) {
                    return
                }
            }
        }

        if (runCommandPreChecks(context)) {
            execute(context)
        }
    }

    open fun destroy() {}

    abstract fun execute(context: Context)

    fun properties() = this.javaClass.getAnnotation(CommandProperties::class.java)

    fun name() = this.javaClass.simpleName

    enum class ExecutionType {
        TRIGGER_CONNECT,
        REQUIRE_MUTUAL,
        STANDARD
    }

}