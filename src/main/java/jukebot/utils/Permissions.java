package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.Database;
import jukebot.JukeBot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

public class Permissions {

    private boolean isDJ(Member m) {
        return m.getRoles().stream().anyMatch(r -> "dj".equalsIgnoreCase(r.getName()));
    }

    private boolean isAlone(Member m) {
        return (m.getVoiceState().inVoiceChannel() && m.getVoiceState().getChannel().getMembers().stream().filter(u -> !u.getUser().isBot()).count() == 1);
    }

    public boolean isElevatedUser(Member m, boolean allowLone) {
        if (allowLone)
            return isAlone(m) || m.isOwner() || JukeBot.botOwnerId == m.getUser().getIdLong() || isDJ(m);

        return m.isOwner() || JukeBot.botOwnerId == m.getUser().getIdLong() || isDJ(m);
    }

    public boolean isTrackRequester(AudioTrack track, long requester) {
        return (long) track.getUserData() == requester;
    }

    public int getTier(long userID) {
        return JukeBot.botOwnerId == userID ? 3 : Database.getTier(userID);
    }

    public boolean canSendTo(TextChannel channel) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS);
    }

    public ConnectionError canConnectTo(VoiceChannel channel) {
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
            return new ConnectionError("Invalid Channel Permissions", "Your VoiceChannel doesn't allow me to Connect/Speak\n\nPlease grant me the 'Connect' and 'Speak' permissions or move to another channel.");

        if (channel.getUserLimit() != 0 && channel.getMembers().size() >= channel.getUserLimit() && !channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_MOVE_OTHERS))
            return new ConnectionError("VoiceChannel Full", "Your VoiceChannel is full. Raise the user limit or grant me the 'Move Members' permission.");

        return null;
    }

    public boolean ensureMutualVoiceChannel(Member m) {
        final AudioManager manager = m.getGuild().getAudioManager();

        return m.getVoiceState().getChannel() != null
                && manager.getConnectedChannel() != null
                && manager.getConnectedChannel().getIdLong() == m.getVoiceState().getChannel().getIdLong();
    }

    public boolean checkVoiceConnection(Member m) {
        final AudioManager manager = m.getGuild().getAudioManager();

        return m.getVoiceState().getChannel() != null &&
                (manager.getConnectedChannel() == null ||
                        manager.getConnectedChannel().getIdLong() == m.getVoiceState().getChannel().getIdLong());
    }

}

