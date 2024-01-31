package me.devoxin.jukebot.commands

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Describe
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.annotations.Checks.DJ
import me.devoxin.jukebot.annotations.Prerequisites.RequireMutualVoiceChannel
import me.devoxin.jukebot.extensions.*
import kotlin.math.ceil

class Queue : Cog {
    override fun name() = "Media"

    @Command(aliases = ["q"], description = "Queue management.", guildOnly = true)
    fun queue(ctx: Context, page: Int = 1) { // This parameter is only usable by text commands.
        view(ctx, page)
    }

    @SubCommand(aliases = ["v"], description = "View the current queue.")
    fun view(ctx: Context, page: Int = 1) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (player.queue.isEmpty()) {
            return ctx.embed("Queue Empty", "There are no tracks in the queue. Why not add some?")
        }

        val queue = player.queue
        val maxPages = ceil(player.queue.size.toDouble() / 10).toInt()
        val targetPage = page.coerceIn(1, maxPages)

        val queueDuration = queue.sumOf { it.duration }.toTimeString()
        val pageItems = buildString {
            val begin = (page - 1) * 10
            val end = minOf(begin + 10, queue.size)

            for ((i, track) in queue.iterate(begin..end)) {
                append("`${i + 1}.` ")
                append("**[${track.info.title} - ${track.info.author}](${track.info.uri})** ")

                if (track.userData as Long == Launcher.shardManager.botId) {
                    appendLine("AutoPlay")
                } else {
                    appendLine("<@${track.userData}>")
                }
            }
        }

        ctx.embed {
            setTitle("Queue (${queue.size} songs, $queueDuration)")
            setDescription(pageItems) // .trim()
            setFooter("Page $targetPage/$maxPages", null)
        }
    }

    @SubCommand(aliases = ["cq", "e", "empty"], description = "Clears the current queue.")
    @DJ(alone = true)
    @RequireMutualVoiceChannel
    fun clear(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (player.queue.isEmpty()) {
            return ctx.embed("Queue Empty", "There are no tracks in the queue.")
        }

        player.queue.clear()
        ctx.embed("Queue Cleared", "The queue is now empty.")
    }

    @SubCommand(aliases = ["dd", "dedupe"], description = "Removes all duplicate tracks from the queue.")
    @DJ(alone = true)
    @RequireMutualVoiceChannel
    fun deduplicate(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (player.queue.isEmpty()) {
            return ctx.embed("Queue Empty", "There are no tracks in the queue.")
        }

        val originalSize = player.queue.size
        val ids = hashSetOf<String>()
        player.queue.removeIf { !ids.add(it.identifier) }

        val removed = originalSize - player.queue.size
        ctx.embed("Queue Cleaned", "`$removed` duplicate tracks were removed from the queue.")
    }

    @SubCommand(aliases = ["m", "mv"], description = "Moves a track in the queue.")
    @DJ(alone = true)
    @RequireMutualVoiceChannel
    fun move(ctx: Context,
             @Describe("The position of the track you want to move.") from: Int,
             @Describe("The position in the queue to move the track to.") to: Int) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (player.queue.isEmpty()) {
            return ctx.embed("Queue Empty", "There are no tracks in the queue.")
        }

        if (from < 0 || from > player.queue.size) {
            return ctx.embed("Move Track", "Invalid `from` position. Example: `/queue move 3 1`")
        }

        if (to < 0 || to > player.queue.size || to == from) {
            return ctx.embed("Move Track", "Invalid `to` position. Example: `/queue move 3 1`")
        }

        val selectedTrack = player.queue.removeAt(from - 1)
        player.queue.add(to - 1, selectedTrack)

        ctx.embed("Track Moved", "**${selectedTrack.info.title}** is now at position **$to** in the queue")
    }

    @SubCommand(aliases = ["ra", "clearabsent"], description = "Removes tracks added by users no longer in the voice channel.")
    @DJ(alone = false)
    @RequireMutualVoiceChannel
    fun removeabsent(ctx: Context) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (player.queue.isEmpty()) {
            return ctx.embed("Queue Empty", "There are no tracks in the queue.")
        }

        val membersInVc = ctx.guild!!.audioManager.connectedChannel!!.members.map { it.idLong }.toSet()
        val tracksToRemove = player.queue.filter { it.userData as Long !in membersInVc }

        player.queue.removeAll(tracksToRemove.toSet())
        ctx.embed("Queue Cleaned", "Removed **${tracksToRemove.size}** tracks queued by absent members.")
    }

    @SubCommand(aliases = ["uq", "unqueue", "r", "rm"], description = "Remove a track from the queue.")
    @DJ(alone = false)
    @RequireMutualVoiceChannel
    fun remove(ctx: Context,
               @Describe("The queue position of the track you want to remove.") at: Int) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (player.queue.isEmpty()) {
            return ctx.embed("Queue Empty", "There are no tracks in the queue.")
        }

        if (at < 0 || at > player.queue.size) {
            return ctx.embed("Remove Track", "You need to specify a number within the range of 1 to ${player.queue.size}.")
        }

        val selectedTrack = player.queue[at - 1]

        if (selectedTrack.userData as Long != ctx.author.idLong && !ctx.isDJ(false)) {
            return ctx.embed("Not a DJ", "You need the DJ role to remove others' tracks.")
        }

        player.queue.removeAt(at - 1)
        ctx.embed("Queue Cleaned", "Removed **${selectedTrack.info.title}** from the queue.")
    }

    @SubCommand(aliases = ["z"], description = "Removes the last track(s) queued by you.")
    @RequireMutualVoiceChannel
    fun undo(ctx: Context,
             @Describe("The maximum number of tracks to remove.") count: Int = 1) { // TODO: implement count
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (player.queue.isEmpty()) {
            return ctx.embed("Queue Empty", "There are no tracks in the queue.")
        }

        val queue = player.queue
        val i = queue.descendingIterator()

        while (i.hasNext()) {
            val t = i.next()
            val requester = t.userData as Long

            if (requester == ctx.author.idLong) {
                i.remove()
                return ctx.embed("Track Removed", "**${t.info.title}** removed from the queue.")
            }
        }

        ctx.embed("No Tracks Found", "No tracks queued by you were found.")
    }
}
