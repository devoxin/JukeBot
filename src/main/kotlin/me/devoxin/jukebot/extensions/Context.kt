package me.devoxin.jukebot.extensions

import me.devoxin.flight.api.context.Context
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.audio.AudioHandler
import me.devoxin.jukebot.integrations.patreon.PatreonTier
import me.devoxin.jukebot.models.PremiumUser
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission.ADMINISTRATOR

val Context.audioPlayer: AudioHandler?
    get() = Launcher.playerManager.players[guild!!.idLong]

val Context.embedColor: Int
    get() = guild?.idLong?.let(Database::getColour) ?: Launcher.config.embedColour.rgb

val Context.premiumUser: PremiumUser?
    get() = when {
        Launcher.isSelfHosted -> PremiumUser.fromTier(author.idLong, PatreonTier.DEVELOPER, shared = false)
        author.idLong in Launcher.commandClient.ownerIds -> PremiumUser.fromTier(author.idLong, PatreonTier.DEVELOPER, shared = false)
        guild != null && Database.getIsPremiumServer(guild!!.idLong) -> PremiumUser.fromTier(author.idLong, PatreonTier.PERSONAL)
        else -> Database.getPatron(author.idLong)
    }

fun Context.audioPlayer() = member?.voiceState?.channel?.let {
    Launcher.playerManager.getOrCreatePlayer(guild!!.idLong, messageChannel.idLong, it.idLong)
} ?: throw IllegalStateException("Cannot create an AudioPlayer with a voice channel ID!")

fun Context.respondUnit(content: String) {
    respond(content)
}

fun Context.embed(create: EmbedBuilder.() -> Unit) {
    respond {
        embed {
            setColor(embedColor)
            apply(create)
        }
    }
}

fun Context.embed(title: String, description: String) {
    embed {
        setTitle(title)
        setDescription(description)
    }
}

fun Context.isDJ(allowLoneVC: Boolean): Boolean {
    val member = member!!

    val customDjRoleId = Database.getDjRole(guild!!.idLong)
    val roleMatch = customDjRoleId?.let { id -> id == guild!!.publicRole.idLong || member.roles.any { it.idLong == id } }
        ?: member.roles.any { it.name.equals("dj", true) }

    val isElevated = member.isOwner || author.idLong in Launcher.commandClient.ownerIds || roleMatch ||
        member.hasPermission(ADMINISTRATOR)

    if (!isElevated && allowLoneVC) {
        return member.voiceState!!.channel?.members?.count { !it.user.isBot } == 1
    }

    return isElevated
}
