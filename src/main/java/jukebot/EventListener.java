package jukebot;

import jukebot.commands.*;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.Arrays;
import java.util.HashMap;

public class EventListener extends ListenerAdapter {

    private final Permissions permissions = new Permissions();
    public static HashMap<String, Command> commands = new HashMap<>();

    EventListener() {
        commands.put("play", new Play());
        commands.put("skip", new Skip());
        commands.put("queue", new Queue());
        commands.put("forceskip", new Forceskip());
        commands.put("invite", new Invite());
        commands.put("help", new Help());
        commands.put("togglepause", new TogglePause());
        commands.put("stop", new Stop());
        commands.put("shuffle", new Shuffle());
        commands.put("now", new Now());
        commands.put("seek", new Seek());
        commands.put("prefix", new Prefix());
        commands.put("volume", new Volume());
        commands.put("donators", new Donators());
        commands.put("save", new Save());
        //commands.put("repeat", new Repeat());
        commands.put("select", new Select());
        commands.put("patreon", new Patreon());
        commands.put("debug", new Debug());
        commands.put("unqueue", new Unqueue());
        commands.put("move", new Move());
        commands.put("scsearch", new ScSearch());
        commands.put("posthere", new PostHere());
        commands.put("clearqueue", new ClearQueue());
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {

        if (!e.getGuild().isAvailable() || e.getAuthor().isBot())
            return;

        final String guildPrefix = Database.getPrefix(e.getGuild().getIdLong());
        final boolean mentioned = e.getMessage().getRawContent().startsWith(e.getGuild().getSelfMember().getAsMention());
        final int triggerLength = mentioned ? e.getGuild().getSelfMember().getAsMention().length() + 1 : guildPrefix.length();

        if (!e.getMessage().getContent().startsWith(guildPrefix) && !mentioned)
            return;

        final String parsed = e.getMessage().getRawContent().substring(triggerLength);
        String command = parsed.split(" +")[0].toLowerCase();
        final String query = parsed.substring(command.length()).trim();

        if (!commands.containsKey(command)) {
            for (Command cmd : commands.values()) {
                if (Arrays.asList(cmd.properties().aliases()).contains(command)) {
                    command = cmd.getClass().getSimpleName().toLowerCase();
                    break;
                }
            }
        }

        Command cmd = commands.get(command);

        if (cmd == null || !permissions.canPost(e.getChannel()))
            return;

        if (cmd.properties().developerOnly() && !permissions.isBotOwner(e.getAuthor().getIdLong()))
            return;

        JukeBot.commandCount++;
        cmd.execute(e, query);

    }

    @Override
    public void onGuildJoin(GuildJoinEvent e) {
        double bots = e.getGuild().getMembers().stream().filter(m -> m.getUser().isBot()).count();
        if (bots / (double) e.getGuild().getMembers().size() > 0.6)
            e.getGuild().leave().queue();
    }

    @Override
    public void onReady(ReadyEvent e) {
        if (JukeBot.botOwnerId == 0L) {
            e.getJDA().asBot().getApplicationInfo().queue(app -> {
                JukeBot.botOwnerId = app.getOwner().getIdLong();

                if (app.getIdLong() != 249303797371895820L && app.getIdLong() != 314145804807962634L) // JukeBot, JukeBot Patron respectively
                    JukeBot.limitationsEnabled = false;
            });
        }
    }

}
