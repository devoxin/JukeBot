package me.devoxin.jukebot.interactions

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.extensions.isNullOr
import net.dv8tion.jda.api.Permission.ADMINISTRATOR
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

open class InteractionBase {
    protected fun checkVoiceChannel(event: ButtonInteractionEvent): Boolean {
        val player = Launcher.playerManager.players[event.guild!!.idLong]

        if (player == null) {
            event.reply("I'm not currently playing anything.").setEphemeral(true).queue()
            return false
        }

        val botChannel = event.guild!!.selfMember.voiceState!!.channel

        if (botChannel == null) {
            event.reply("I'm not currently playing anything.").setEphemeral(true).queue()
            return false
        }

        if (event.member!!.voiceState!!.channel.isNullOr { it.idLong != botChannel.idLong }) {
            event.reply("You're not in my voice channel.").setEphemeral(true).queue()
            return false
        }

        return true
    }

    protected fun isDJ(member: Member, allowLoneVC: Boolean): Boolean {
        val customDjRoleId = Database.getDjRole(member.guild.idLong)
        val roleMatch = customDjRoleId?.let { id -> id == member.guild.publicRole.idLong || member.roles.any { it.idLong == id } }
            ?: member.roles.any { it.name.equals("dj", true) }

        val isElevated = member.isOwner || member.idLong in Launcher.commandClient.ownerIds || roleMatch ||
            member.hasPermission(ADMINISTRATOR)

        if (!isElevated && allowLoneVC) {
            return member.voiceState!!.channel?.members?.count { !it.user.isBot } == 1
        }

        return isElevated
    }
}
