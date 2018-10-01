package jukebot.utils;

import jukebot.Database;
import jukebot.JukeBot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

public class Permissions {

    public int getTier(long userID) {
        return JukeBot.botOwnerId == userID ? 3 : Database.getTier(userID);
    }

    public boolean canSendTo(TextChannel channel) {
        return channel.canTalk() && channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS);
    }

    public static ConnectionError canConnectTo(VoiceChannel channel) {
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
            return new ConnectionError("Invalid Channel Permissions", "I need the 'Connect' and 'Speak' permissions to play in that channel.");

        if (channel.getUserLimit() != 0 && channel.getMembers().size() >= channel.getUserLimit() && !channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_MOVE_OTHERS))
            return new ConnectionError("VoiceChannel Full", "VoiceChannel user limit exceeded. Raise the user limit or move to another channel.");

        return null;
    }

    public boolean ensureMutualVoiceChannel(Member m) {
        final AudioManager manager = m.getGuild().getAudioManager();

        return m.getVoiceState().getChannel() != null
                && manager.getConnectedChannel() != null
                && manager.getConnectedChannel().getIdLong() == m.getVoiceState().getChannel().getIdLong();
    }

}

