package jukebot;

import jukebot.commands.*;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;

import static jukebot.utils.Bot.LOG;

public class EventListener extends ListenerAdapter {

    private final Permissions permissions = new Permissions();
    private final DatabaseHandler db = new DatabaseHandler();
    private static HashMap<String, Command> commands = new HashMap<>();
    private static HashMap<String, String> aliases = new HashMap<>();

    public EventListener() {
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
        commands.put("fastforward", new FastForward());
        commands.put("prefix", new Prefix());
        commands.put("volume", new Volume());
        commands.put("donators", new Donators());
        commands.put("save", new Save());
        commands.put("repeat", new Repeat());
        commands.put("select", new Select());
        commands.put("patreon", new Patreon());
        commands.put("debug", new Debug());
        commands.put("unqueue", new Unqueue());
        commands.put("move", new Move());
        commands.put("scsearch", new ScSearch());
        commands.put("posthere", new PostHere());

        aliases.put("p", "play");
        aliases.put("q", "queue");
        aliases.put("fs", "forceskip");
        aliases.put("tp", "togglepause");
        aliases.put("n", "now");
        aliases.put("np", "now");
        aliases.put("ff", "fastforward");
        aliases.put("vol", "volume");
        aliases.put("sel", "select");
        aliases.put("uq", "unqueue");
        aliases.put("m", "move");
        aliases.put("sc", "scsearch");
        aliases.put("ph", "posthere");
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {

        if (!e.getGuild().isAvailable() || e.getAuthor().isBot())
            return;

        final String prefix = db.getPrefix(e.getGuild().getIdLong());

        if (!e.getMessage().getContent().startsWith(prefix) && !e.getMessage().getMentionedUsers().contains(e.getJDA().getSelfUser()))
            return;

        if (e.getMessage().getMentionedUsers().contains(e.getJDA().getSelfUser()) && permissions.canPost(e.getChannel())) {
            LOG.debug("Received mention from " + e.getAuthor().getName());
            if (e.getMessage().getContent().contains("help")) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Mention | Help")
                        .setDescription("Server Prefix: " + db.getPrefix(e.getGuild().getIdLong()) + "\n\nYou can reset the prefix using '@" + e.getJDA().getSelfUser().getName() + " rp'")
                        .build()
                ).queue();
            }

            if (e.getMessage().getContent().contains("rp") && permissions.canPost(e.getChannel())) {
                if (!permissions.isElevatedUser(e.getMember(), false)) {
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Mention | Prefix Reset")
                            .setDescription("You do not have permission to reset the prefix. (Requires DJ role)")
                            .build()
                    ).queue();
                }
                final boolean result = db.setPrefix(e.getGuild().getIdLong(), db.getPropertyFromConfig("prefix"));
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Mention | Prefix Reset")
                        .setDescription(result ? "Server prefix reset to '" + db.getPropertyFromConfig("prefix") + "'" : "Failed to reset prefix")
                        .build()
                ).queue();
            }
            return;
        }

        String command = e.getMessage().getContent().split(" ")[0].substring(prefix.length()).toLowerCase();
        String query = e.getMessage().getContent().contains(" ") ? e.getMessage().getContent().split(" ", 2)[1] : "";

        if (aliases.containsKey(command))
            command = aliases.get(command);

        if (!commands.containsKey(command))
            return;

        if (!permissions.canPost(e.getChannel())) {
            e.getAuthor().openPrivateChannel().queue(dm ->
                dm.sendMessage("I cannot send messages/embed links in " + e.getChannel().getAsMention() + "\nSwitch to another channel.")
                        .queue(null, error -> LOG.warn("Couldn't DM " + e.getAuthor().getName()))
            );
            return;
        }

        LOG.debug("Executing command '" + command + "' with args '" + query + "' from " + e.getAuthor().getName());
        commands.get(command).execute(e, query);

    }

    @Override
    public void onGuildJoin(GuildJoinEvent e) {

        double bots = e.getGuild().getMembers().stream().filter(m -> m.getUser().isBot()).count();
        if (bots / (double) e.getGuild().getMembers().size() > 0.6)
            e.getGuild().leave().queue();

    }

    @Override
    public void onReady(ReadyEvent e) {

        if (Bot.BotOwnerID == 0L)
            e.getJDA().asBot().getApplicationInfo().queue(app -> Bot.BotOwnerID = app.getOwner().getIdLong());

    }

}
