package jukebot.utils;

import jukebot.DatabaseHandler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.util.List;

public class Permissions {

    private final DatabaseHandler db = new DatabaseHandler();

    private boolean hasRole(Member m, String r) {

        final List<Role> FoundRoles = m.getGuild().getRolesByName(r, false);
        Role fRole = FoundRoles.isEmpty() ? null : FoundRoles.get(0);

        return fRole != null && m.getRoles().contains(fRole);

    }

    public boolean isBotOwner(String userID) {
        return db.getPropertyFromConfig("owners").contains(userID);
    }

    /*public boolean isBlocked(Member m) {
        return hasRole(m, "NoMusic") && !m.isOwner() && !isBotOwner(m.getUser().getId());
    }*/

    public boolean isElevatedUser(Member m, boolean AllowLone) {
        if (AllowLone)
            return isALoner(m) || m.isOwner() || hasRole(m, "DJ") || isBotOwner(m.getUser().getId());
        else
            return m.isOwner() || hasRole(m, "DJ") || isBotOwner(m.getUser().getId());
    }

    public boolean isALoner(Member m) {
        return (m.getVoiceState().inVoiceChannel() && m.getVoiceState().getChannel().getMembers().stream().filter(u -> !u.getUser().isBot()).count() == 1);
    }

    public boolean isBaller(String userID, int tier) {
        return isBotOwner(userID) || Integer.parseInt(db.getTier(Long.parseLong(userID))) >= tier;
    }

    public boolean canPost(TextChannel channel) {
        return channel != null && channel.canTalk() && channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS);
    }

    public boolean canConnect(VoiceChannel channel) {
        return channel != null && channel.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK);
    }


}
