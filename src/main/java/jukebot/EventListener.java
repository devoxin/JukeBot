package jukebot;

import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class EventListener extends ListenerAdapter {

    private final Permissions permissions = new Permissions();
    public static HashMap<String, Command> commands = new HashMap<>();

    EventListener() {
        final Reflections loader = new Reflections("jukebot.commands");

        Set<Class<?>> discoveredCommands = loader.getTypesAnnotatedWith(CommandProperties.class);
        JukeBot.LOG.info("Discovered " + discoveredCommands.size() + " commands");

        for (Class klass : discoveredCommands) {
            try {
                Command cmd = (Command) klass.newInstance();

                if (!cmd.properties().enabled())
                    continue;

                commands.put(cmd.name().toLowerCase(), cmd);
            } catch (InstantiationException | IllegalAccessException e) {
                JukeBot.LOG.error("An error occurred while creating a new instance of command '" + klass.getName() + "'");
            }
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {

        if (!e.getGuild().isAvailable() || e.getAuthor().isBot() || !permissions.canPost(e.getChannel()))
            return;

        final String guildPrefix = Database.getPrefix(e.getGuild().getIdLong());
        final boolean mentioned = e.getMessage().getContentRaw().startsWith(e.getGuild().getSelfMember().getAsMention());
        final int triggerLength = mentioned ? e.getGuild().getSelfMember().getAsMention().length() + 1 : guildPrefix.length();

        if (!e.getMessage().getContentDisplay().startsWith(guildPrefix) && !mentioned)
            return;

        final String parsed = e.getMessage().getContentRaw().substring(triggerLength);
        final String command = parsed.split(" +")[0].toLowerCase();
        final String query = parsed.substring(command.length()).trim();

        Command cmd = commands.get(command);

        if (cmd == null) {
            for (Command c : commands.values()) {
                if (Arrays.asList(c.properties().aliases()).contains(command)) {
                    cmd = commands.get(c.name().toLowerCase());
                    break;
                }
            }
        }


        if (cmd == null || cmd.properties().developerOnly() && !permissions.isBotOwner(e.getAuthor().getIdLong()))
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
