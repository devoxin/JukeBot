package jukebot.utils;

import jukebot.DatabaseHandler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.List;

public class Permissions {

    private final DatabaseHandler db = new DatabaseHandler();

    public boolean isBotOwner(long userID) {
        return userID == Bot.BotOwnerID;
    }

    private boolean hasDJRole(Member m) {
        return m.getRoles().stream().anyMatch(r -> "dj".equalsIgnoreCase(r.getName()));
    }

    public boolean isElevatedUser(Member m, boolean AllowLone) {
        if (AllowLone)
            return isALoner(m) || m.isOwner() || isBotOwner(m.getUser().getIdLong()) || hasDJRole(m);
        else
            return m.isOwner() || isBotOwner(m.getUser().getIdLong()) || hasDJRole(m);
    }

    private boolean isALoner(Member m) {
        return (m.getVoiceState().inVoiceChannel() && m.getVoiceState().getChannel().getMembers().stream().filter(u -> !u.getUser().isBot()).count() == 1);
    }

    public boolean isBaller(long userID, int tier) {
        return getTierLevel(userID) >= tier;
    }

    int getTierLevel(long userID) {
        return isBotOwner(userID) ? 3 : db.getTier(userID);
    }

    public boolean canPost(TextChannel channel) {
        return channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS);
    }

    public CONNECT_STATUS canConnect(VoiceChannel channel) {
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
            return CONNECT_STATUS.NO_CONNECT_SPEAK;

        if (channel.getUserLimit() != 0 && channel.getMembers().size() >= channel.getUserLimit() && !channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_MOVE_OTHERS))
            return CONNECT_STATUS.USER_LIMIT;

        return CONNECT_STATUS.CONNECT;
    }

    public boolean checkVoiceChannel(Member m) {
        final AudioManager manager = m.getGuild().getAudioManager();

        Bot.LOG.debug("Member OK: " + (m != null) + " | M-VC OK: " + (m.getVoiceState().inVoiceChannel()) + " | CONNECTED: " + (manager.isConnected() || manager.isAttemptingToConnect()));

        return m.getVoiceState().inVoiceChannel() && (!manager.isAttemptingToConnect() && !manager.isConnected() || manager.getConnectedChannel().getIdLong() == m.getVoiceState().getChannel().getIdLong());
    }

    public enum CONNECT_STATUS {
        NO_CONNECT_SPEAK,
        USER_LIMIT,
        CONNECT
    }

}
