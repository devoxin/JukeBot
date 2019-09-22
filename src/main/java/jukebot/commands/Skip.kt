package jukebot.commands

import jukebot.Database
import jukebot.framework.Command
import jukebot.framework.CommandCategory
import jukebot.framework.CommandProperties
import jukebot.framework.Context

@CommandProperties(description = "Vote to skip the track", category = CommandCategory.CONTROLS)
class Skip : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {

        val player = context.getAudioPlayer()

        if (!player.isPlaying) {
            return context.embed("Not Playing", "Nothing is currently playing.")
        }

        if (!context.ensureMutualVoiceChannel()) {
            return
        }

        val totalVotes = player.voteSkip(context.author.idLong)
        val voteThreshold = Database.getSkipThreshold(context.guild.idLong)

        val neededVotes = Math.ceil(context.guild.audioManager.connectedChannel!!
            .members
            .stream()
            .filter { u -> !u.user.isBot }
            .count() * voteThreshold).toInt()

        if (neededVotes - totalVotes <= 0) {
            player.playNext()
        } else {
            context.embed("Vote Acknowledged", "${(neededVotes - totalVotes)} votes needed to skip.")
        }
    }
}