package jukebot;

import jukebot.commands.*;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;

public class EventListener extends ListenerAdapter {

    private final Permissions permissions = new Permissions();
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

        final String prefix = Database.getPrefix(e.getGuild().getIdLong());

        if (!e.getMessage().getContent().startsWith(prefix) && !e.getMessage().isMentioned(e.getJDA().getSelfUser()))
            return;

        if (e.getMessage().isMentioned(e.getJDA().getSelfUser()) && permissions.canPost(e.getChannel())) {
            if (e.getMessage().getContent().contains("help")) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.EmbedColour)
                        .setTitle("Mention | Help")
                        .setDescription("Server Prefix: **" + Database.getPrefix(e.getGuild().getIdLong()) + "**\n\nYou can reset the prefix using `@" + e.getJDA().getSelfUser().getName() + " rp`")
                        .build()
                ).queue();
            }

            if (e.getMessage().getContent().contains("rp") && permissions.canPost(e.getChannel())) {
                if (!permissions.isElevatedUser(e.getMember(), false)) {
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(JukeBot.EmbedColour)
                            .setTitle("Mention | Prefix Reset")
                            .setDescription("You do not have permission to reset the prefix. (Requires DJ role)")
                            .build()
                    ).queue();
                }
                final boolean result = Database.setPrefix(e.getGuild().getIdLong(), Database.getPropertyFromConfig("prefix"));
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.EmbedColour)
                        .setTitle("Mention | Prefix Reset")
                        .setDescription(result ? "Server prefix reset to **" + Database.getPropertyFromConfig("prefix") + "**" : "Failed to reset prefix")
                        .build()
                ).queue();
            }
            return;
        }

        //String command = e.getMessage().getContent().substring(prefix.length()).trim().split(" ")[0].toLowerCase(); // Spaced prefixes, anyone?
        String command = e.getMessage().getContent().substring(prefix.length()).split(" ")[0].toLowerCase();
        // Fun fact, using substring instead of split is faster; when parsing the query 100,000,000 times, 'split' would be faster by ~300ms
        // where there were no additional arguments in the message, but performed up to 7 seconds slower when a single argument was present
        final String query = e.getMessage().getContent().substring(prefix.length() + command.length()).trim();

        if (aliases.containsKey(command))
            command = aliases.get(command);

        if (!commands.containsKey(command))
            return;

        if (!permissions.canPost(e.getChannel())) {
            e.getAuthor().openPrivateChannel().queue(dm ->
                dm.sendMessage("I cannot send messages/embed links in " + e.getChannel().getAsMention() + "\nSwitch to another channel.")
                        .queue()
            );
            return;
        }

        final long startTime = System.currentTimeMillis();
        commands.get(command).execute(e, query);
        JukeBot.LOG.debug("[" + command.toUpperCase() + "] execution time: " + (System.currentTimeMillis() - startTime) + "ms");

    }

    @Override
    public void onGuildJoin(GuildJoinEvent e) {
        double bots = e.getGuild().getMembers().stream().filter(m -> m.getUser().isBot()).count();
        if (bots / (double) e.getGuild().getMembers().size() > 0.6)
            e.getGuild().leave().queue();
    }

    @Override
    public void onReady(ReadyEvent e) {
        if (JukeBot.BotOwnerID == 0L)
            e.getJDA().asBot().getApplicationInfo().queue(app -> JukeBot.BotOwnerID = app.getOwner().getIdLong());
    }

}
