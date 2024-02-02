package me.devoxin.jukebot.interactions

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.annotations.ComponentId
import me.devoxin.jukebot.annotations.InteractionHandler
import me.devoxin.jukebot.utils.Components
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.ceil

@InteractionHandler
class NowPlayingButtons : InteractionBase() {
    @ComponentId("np_prev")
    fun previousButton(event: ButtonInteractionEvent) {
        if (!checkVoiceChannel(event)) {
            return
        }

        if (!isDJ(event.member!!, allowLoneVC = true)) {
            return
        }

        val player = Launcher.playerManager.players[event.guild!!.idLong]
            ?: return

        if (!player.canGoBack) {
            return event.reply("There's no previous track to go back to.").setEphemeral(true).queue()
        }

        player.previous()

        event.reply("${event.user.asMention} has gone back to the previous track!")
            .setSuppressedNotifications(true)
            .delay(10, SECONDS)
            .flatMap(InteractionHook::deleteOriginal)
            .queue(null) {}
    }

    @ComponentId("np_pause")
    fun pauseButton(event: ButtonInteractionEvent) {
        if (!checkVoiceChannel(event)) {
            return
        }

        if (!isDJ(event.member!!, allowLoneVC = true)) {
            return
        }

        val player = Launcher.playerManager.players[event.guild!!.idLong]
            ?: return

        if (player.player.playingTrack == null) {
            return event.reply("I'm not currently playing anything.").setEphemeral(true).queue()
        }

        val setting = !player.player.isPaused
        player.player.isPaused = setting

        event.editComponents(if (setting) Components.nowPlayingRowPaused else Components.nowPlayingRowUnpaused).queue()
        event.hook.sendMessage("${event.user.asMention} has ${if (setting) "paused" else "resumed"} the player.")
            .setSuppressedNotifications(true)
            .delay(10, SECONDS)
            .flatMap(Message::delete)
            .queue(null) {}
    }

    @ComponentId("np_next")
    fun nextButton(event: ButtonInteractionEvent) {
        if (!checkVoiceChannel(event)) {
            return
        }

        val isDj = isDJ(event.member!!, allowLoneVC = true)

        val player = Launcher.playerManager.players[event.guild!!.idLong]
            ?: return

        val current = player.player.playingTrack
            ?: return event.reply("I'm not currently playing anything.").setEphemeral(true).queue()

        if (isDj || current.userData as Long == event.user.idLong) {
            player.next()
            event.reply("${event.user.asMention} has force-skipped the track.")
                .setSuppressedNotifications(true)
                .delay(10, SECONDS)
                .flatMap(InteractionHook::deleteOriginal)
                .queue(null) {}
        } else {
            val totalVotes = player.voteSkip(event.user.idLong)
            val voteThreshold = Database.getSkipThreshold(event.guild!!.idLong)
            val neededVotes = ceil(event.guild!!.audioManager.connectedChannel!!.members.count { !it.user.isBot } * voteThreshold).toInt()

            if (totalVotes >= neededVotes) {
                event.reply("${event.user.asMention} has voted to skip.\nVoting has passed. The track will be skipped.")
                    .setSuppressedNotifications(true)
                    .queue()

                player.next()
            } else {
                event.reply("${event.user.asMention} has voted to skip.\n${(neededVotes - totalVotes)} votes needed to skip.")
                    .setSuppressedNotifications(true)
                    .queue()
            }
        }
    }
}
