package jukebot.framework

import net.dv8tion.jda.api.Permission

abstract class Command(private val executionType: ExecutionType) {

    open fun runChecks(context: Context) {
        when (executionType) {
            ExecutionType.STANDARD -> execute(context)
            ExecutionType.REQUIRE_MUTUAL -> {
                val audioManager = context.guild.audioManager
                val isConnected = audioManager.connectedChannel != null
                val memberVoice = context.member.voiceState!!
                val channelName = audioManager.connectedChannel?.name

                if (memberVoice.channel == null) {
                    if (isConnected) {
                        context.embed("No Mutual VoiceChannel", "You need to join **$channelName**!")
                    } else {
                        context.embed("No VoiceChannel", "You need to join a VoiceChannel!")
                    }
                } else {
                    if (isConnected && memberVoice.channel?.idLong != audioManager.connectedChannel?.idLong) {
                        return context.embed("No Mutual VoiceChannel", "You need to join **$channelName**!")
                    }

                    execute(context)
                }
            }
            ExecutionType.TRIGGER_CONNECT -> {
                if (context.argString.isEmpty()) {
                    return context.embed(name(), "You need to specify an identifier to lookup.")
                }

                val audioManager = context.guild.audioManager
                val isConnected = audioManager.connectedChannel != null
                val memberVoice = context.member.voiceState!!
                val channelName = audioManager.connectedChannel?.name

                if (memberVoice.channel == null) {
                    if (isConnected) {
                        context.embed("No Mutual VoiceChannel", "You need to join **$channelName**!")
                    } else {
                        context.embed("No VoiceChannel", "You need to join a VoiceChannel!")
                    }

                    return
                }

                if (!isConnected) {
                    val voiceChannel = memberVoice.channel!!

                    if (!voiceChannel.guild.selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)) {
                        return context.embed(
                                "Unable to Connect",
                                "The VoiceChannel permissions prevent me from connecting.\n" +
                                        "Check that I have the `Connect` and `Speak` permissions."
                        )
                    }

                    if (voiceChannel.userLimit != 0 && voiceChannel.members.size >= voiceChannel.userLimit &&
                            !voiceChannel.guild.selfMember.hasPermission(Permission.VOICE_MOVE_OTHERS)) {
                        return context.embed(
                                "Unable to Connect",
                                "Your VoiceChannel is currently full.\n" +
                                        "Raise the user limit, or move to another channel."
                        )
                    }

                    audioManager.openAudioConnection(memberVoice.channel)
                } else if (memberVoice.channel?.idLong != audioManager.connectedChannel?.idLong) {
                    return context.embed("No Mutual VoiceChannel", "You need to join **$channelName**!")
                }

                execute(context)
            }
        }
    }

    abstract fun execute(context: Context)

    fun properties() = this.javaClass.getAnnotation(CommandProperties::class.java)

    fun name() = this.javaClass.simpleName

    enum class ExecutionType {
        TRIGGER_CONNECT,
        REQUIRE_MUTUAL,
        STANDARD
    }

}