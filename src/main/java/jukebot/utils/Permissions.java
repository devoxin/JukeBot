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

    private boolean hasRole(Member m, String r) {

        final List<Role> FoundRoles = m.getGuild().getRolesByName(r, false);
        Role fRole = FoundRoles.isEmpty() ? null : FoundRoles.get(0);

        return fRole != null && m.getRoles().contains(fRole);

    }

    public boolean isBotOwner(long userID) {
        return userID == Bot.BotOwnerID;
    }

    public boolean isElevatedUser(Member m, boolean AllowLone) {
        if (AllowLone)
            return isALoner(m) || m.isOwner() || hasRole(m, "DJ") || isBotOwner(m.getUser().getIdLong());
        else
            return m.isOwner() || hasRole(m, "DJ") || isBotOwner(m.getUser().getIdLong());
    }

    private boolean isALoner(Member m) {
        return (m.getVoiceState().inVoiceChannel() && m.getVoiceState().getChannel().getMembers().stream().filter(u -> !u.getUser().isBot()).count() == 1);
    }

    public boolean isBaller(long userID, int tier) {
        return getTierLevel(userID) >= tier;
    }

    int getTierLevel(long userID) {
        return isBotOwner(userID) ? 3 : Integer.parseInt(db.getTier(userID));
    }

    public boolean canPost(TextChannel channel) {
        return channel.canTalk() && channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS);
    }

    public CONNECT_STATUS canConnect(VoiceChannel channel) {
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
            return CONNECT_STATUS.NO_CONNECT_SPEAK;

        if (channel.getUserLimit() != 0 && channel.getMembers().size() >= channel.getUserLimit() && !channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_MOVE_OTHERS))
            return CONNECT_STATUS.USER_LIMIT;

        return CONNECT_STATUS.CONNECT;
    }

    boolean CheckVoiceChannel(Member m) {
        final AudioManager manager = m.getGuild().getAudioManager();

        return m.getVoiceState().inVoiceChannel() && (!manager.isAttemptingToConnect() && !manager.isConnected() || manager.getConnectedChannel().getId().equalsIgnoreCase(m.getVoiceState().getChannel().getId()));

    }

    public enum CONNECT_STATUS {
        NO_CONNECT_SPEAK,
        USER_LIMIT,
        CONNECT
    }

}
