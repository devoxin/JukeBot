package jukebot;

import jukebot.commands.*;
import jukebot.commands.Queue;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;
import java.util.HashMap;

public class EventListener extends ListenerAdapter {

    private final Permissions permissions = new Permissions();
    private final DatabaseHandler db = new DatabaseHandler();
    private static HashMap<String, Command> commands = new HashMap<>();
    private static HashMap<String, String> aliases = new HashMap<>();

    private static boolean hasFired = false;

    EventListener() {
        commands.put("play", new Play());
        commands.put("skip", new Skip());
        commands.put("queue", new Queue());
        commands.put("forceskip", new Forceskip());
        commands.put("invite", new Invite());
        commands.put("help", new Help());
        commands.put("pause", new Pause());
        commands.put("resume", new Resume());
        commands.put("stop", new Stop());
        commands.put("shuffle", new Shuffle());
        commands.put("now", new Now());
        commands.put("fastforward", new FastForward());
        commands.put("prefix", new Prefix());
        commands.put("volume", new Volume());
        commands.put("manage", new Manage());
        commands.put("save", new Save());
        commands.put("repeat", new Repeat());
        commands.put("select", new Select());
        commands.put("patreon", new Patreon());
        commands.put("debug", new Debug());
        commands.put("reset", new Reset());
        commands.put("unqueue", new Unqueue());
        commands.put("move", new Move());

        aliases.put("p", "play");
        aliases.put("q", "queue");
        aliases.put("fs", "forceskip");
        aliases.put("r", "resume");
        aliases.put("ps", "pause");
        aliases.put("n", "now");
        aliases.put("ff", "fastforward");
        aliases.put("vol", "volume");
        aliases.put("sel", "select");
        aliases.put("uq", "unqueue");
        aliases.put("m", "move");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {

        if (e.getGuild() == null || !e.getGuild().isAvailable() || e.getAuthor().isBot())
            return;

        final String prefix = db.getPrefix(e.getGuild().getIdLong());

        if (!e.getMessage().getContent().startsWith(prefix) && !e.getMessage().getMentionedUsers().contains(e.getJDA().getSelfUser()))
            return;

        if (e.getMessage().getMentionedUsers().contains(e.getJDA().getSelfUser()) && permissions.canPost(e.getTextChannel())) {
            if (e.getMessage().getContent().contains("help")) {
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Mention | Help")
                        .setDescription("Server Prefix: " + db.getPrefix(e.getGuild().getIdLong()) + "\n\nYou can reset the prefix using '@" + e.getJDA().getSelfUser().getName() + " rp'")
                        .build()
                ).queue();
            }

            if (e.getMessage().getContent().contains("rp") && permissions.canPost(e.getTextChannel())) {
                if (!permissions.isElevatedUser(e.getMember(), false)) {
                    e.getTextChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Mention | Prefix Reset")
                            .setDescription("You do not have permission to reset the prefix. (Requires DJ role)")
                            .build()
                    ).queue();
                }
                final boolean result = db.setPrefix(e.getGuild().getIdLong(), db.getPropertyFromConfig("prefix"));
                e.getTextChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Mention | Prefix Reset")
                        .setDescription(result ? "Server prefix reset to '" + db.getPropertyFromConfig("prefix") + "'" : "Failed to reset prefix")
                        .build()
                ).queue();
            }
            return;
        }

        String command = e.getMessage().getContent().split(" ")[0].substring(prefix.length()).toLowerCase();
        String query = e.getMessage().getContent().split(" ").length > 1 ? e.getMessage().getContent().split(" ", 2)[1] : "";

        if (aliases.containsKey(command))
            command = aliases.get(command);

        if (!commands.containsKey(command))
            return;

        if (!permissions.canPost(e.getTextChannel())) {
            final PrivateChannel DMChannel = e.getAuthor().openPrivateChannel().complete();
            DMChannel.sendMessage("I cannot send messages/embed links in " + e.getTextChannel().getAsMention() + "\nSwitch to another channel.")
                    .queue(null, error -> System.out.println("Unable to DM " + e.getAuthor().getName() + "\n" + error.getMessage()));
            return;
        }

        commands.get(command.toLowerCase()).execute(e, query);

        super.onMessageReceived(e);
    }

    @Override
    public void onGuildJoin(GuildJoinEvent e) {

        double bots = e.getGuild().getMembers().stream().filter(m -> m.getUser().isBot()).count();
        if (bots / (double) e.getGuild().getMembers().size() > 0.6)
            e.getGuild().leave().queue();

        super.onGuildJoin(e);
    }

    @Override
    public void onReady(ReadyEvent e) {
        if (!hasFired && e.getJDA().getSelfUser().getId().equals("314145804807962634")) {
            hasFired = true;
            Bot.EmbedColour = Color.decode("#FDD744");
        }


        super.onReady(e);
    }

}
