package jukebot.framework

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

            if (voiceChannel.userLimit != 0 && voiceChannel.members.size >= voiceChannel.userLimit &&
                    !voiceChannel.guild.selfMember.hasPermission(Permission.VOICE_MOVE_OTHERS)) {
                context.embed(
                        "Unable to Connect",
                        "Your VoiceChannel is currently full.\n" +
                                "Raise the user limit, or move to another channel."
                )
                return false
            }

            audioManager.openAudioConnection(memberVoice.channel)
        }

        return true
    }

    open fun runChecks(context: Context) {
        when (executionType) {
            ExecutionType.STANDARD -> execute(context)
            ExecutionType.REQUIRE_MUTUAL -> {
                if (checkVoiceState(context, true)) {
                    execute(context)
                }
            }
            ExecutionType.TRIGGER_CONNECT -> {
                if (context.argString.isEmpty() && context.message.attachments.size == 0) {
                    return context.embed(name(), "You need to specify an identifier to lookup.")
                }

                if (connectToChannel(context)) {
                    execute(context)
                }
            }
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