package jukebot.commands

import jukebot.Database
import jukebot.framework.*
import kotlin.math.ceil

@CommandProperties(aliases = ["next"], description = "Vote to skip the track", category = CommandCategory.CONTROLS)
@CommandCheck(isPlaying = true)
class Skip : Command(ExecutionType.REQUIRE_MUTUAL) {

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        val totalVotes = player.voteSkip(context.author.idLong)
        val voteThreshold = Database.getSkipThreshold(context.guild.idLong)

        val neededVotes = ceil(context.guild.audioManager.connectedChannel!!
            .members
            .filter { !it.user.isBot }
            .size * voteThreshold).toInt()

        if (neededVotes - totalVotes <= 0) {
            player.playNext()
        } else {
            context.embed("Vote Acknowledged", "${(neededVotes - totalVotes)} votes needed to skip.")
        }
    }
}