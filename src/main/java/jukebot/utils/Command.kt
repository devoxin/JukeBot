package jukebot.utils

abstract class Command(private val executionType: ExecutionType) {

    open fun runChecks(context: Context) {
        when (executionType) {
            ExecutionType.STANDARD -> execute(context)
            ExecutionType.REQUIRE_MUTUAL -> {
                val audioManager = context.guild.audioManager
                val isConnected = audioManager.connectedChannel != null
                val memberVoice = context.member.voiceState
                val channelName = audioManager.connectedChannel?.name

                if (memberVoice.channel == null) {
                    if (isConnected) {
                        context.embed("No Mutual VoiceChannel", "You need to join **$channelName**!")
                    } else {
                        context.embed("No VoiceChannel", "You need to join a VoiceChannel!")
                    }
                } else {
                    if (memberVoice.channel.idLong != audioManager.connectedChannel.idLong) {
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
                val memberVoice = context.member.voiceState
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
                    val connectionError: ConnectionError? = Permissions.canConnectTo(memberVoice.channel)

                    if (connectionError != null) {
                        return context.embed(connectionError.title, connectionError.description)
                    }

                    audioManager.openAudioConnection(memberVoice.channel)
                } else if (memberVoice.channel.idLong != audioManager.connectedChannel.idLong) {
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