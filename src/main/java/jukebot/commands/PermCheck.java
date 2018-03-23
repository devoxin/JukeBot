package jukebot.commands;

import jukebot.utils.CommandProperties;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

@CommandProperties(description = "Check the current channel permissions", aliases = {"pc"})
public class PermCheck implements Command {

    Permissions perms = new Permissions();

    @Override
    public void execute(GuildMessageReceivedEvent e, String query) {
        List<Permission> textPerms = e.getGuild().getSelfMember().getPermissions(e.getChannel());
        List<Permission> voicePerms = null;

        if (e.getMember().getVoiceState().getChannel() != null)
            voicePerms = e.getGuild().getSelfMember().getPermissions(e.getMember().getVoiceState().getChannel());

        StringBuilder sb = new StringBuilder("**This is a tool used for diagnosing lack of responses from JukeBot.**\n\n");

        sb.append("**Permissions for TextChannel <#").append(e.getChannel().getId()).append(">**\n```\n");

        for (Permission p : textPerms) {
            if (!p.isText()) continue;
            sb.append(p.name()).append("\n");
        }

        boolean canUseChat = perms.canSendTo(e.getChannel());
        sb.append("```\n\n**Valid permissions for text:** ").append(canUseChat ? "Yes" : "No").append("\n\n");

        if (voicePerms != null) {
            sb.append("\n\n**Permissions for VoiceChannel ").append(e.getMember().getVoiceState().getChannel().getName()).append("**\n```\n");

            for (Permission p : voicePerms) {
                if (!p.isVoice()) continue;
                sb.append(p.name()).append("\n");
            }

            boolean canUseVoice = perms.canConnectTo(e.getMember().getVoiceState().getChannel()) == null;
            sb.append("```\n\n**Valid permissions for voice:** ").append(canUseVoice ? "Yes" : "No");
        }


        e.getAuthor().openPrivateChannel().queue(channel ->
            channel.sendMessage(sb.toString().trim()).queue(null, err -> {})
        );
    }

}
