package me.devoxin.jukebot.commands.controls

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.framework.*
import kotlin.math.ceil

@CommandProperties(aliases = ["next"], description = "Vote to skip the track", category = CommandCategory.CONTROLS, slashCompatible = true)
@CommandChecks.Playing
class Skip : Command(ExecutionType.REQUIRE_MUTUAL) {
    override fun execute(context: Context) {
        val player = context.audioPlayer

        val totalVotes = player.voteSkip(context.author.idLong)
        val voteThreshold = Database.getSkipThreshold(context.guild.idLong)
        val neededVotes = ceil(context.guild.audioManager.connectedChannel!!.members.count { !it.user.isBot } * voteThreshold).toInt()

        if (neededVotes - totalVotes <= 0) {
            player.playNext()
        } else {
            context.embed("Vote Acknowledged", "${(neededVotes - totalVotes)} votes needed to skip.")
        }
    }
}
