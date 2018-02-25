package jukebot;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class EventListener extends ListenerAdapter {

    private final Permissions permissions = new Permissions();
    public static ArrayList<Command> commands = new ArrayList<>();

    EventListener() {
        final Reflections loader = new Reflections("jukebot.commands");

        loader.getTypesAnnotatedWith(CommandProperties.class).forEach(command -> {
            try {
                final Command cmd = (Command) command.newInstance();

                if (cmd.properties().enabled())
                    commands.add(cmd);

            } catch (InstantiationException | IllegalAccessException e) {
                JukeBot.LOG.error("An error occurred while creating a new instance of command '" + command.getName() + "'");
            }
        });
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

        if (mentioned && !e.getMessage().getContentRaw().contains(" "))
            return;

        final String parsed = e.getMessage().getContentRaw().substring(triggerLength);
        final String command = parsed.trim().split("\\s+")[0].toLowerCase();
        final String query = parsed.substring(command.length()).trim();

        final Command cmd = commands.stream()
                    .filter(c -> c.name().equalsIgnoreCase(command) || Arrays.asList(c.properties().aliases()).contains(command))
                    .findFirst()
                    .orElse(null);

        if (cmd == null || cmd.properties().developerOnly() && !permissions.isBotOwner(e.getAuthor().getIdLong()))
            return;

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

                if (app.getIdLong() != 249303797371895820L && app.getIdLong() != 314145804807962634L) { // JukeBot, JukeBot Patron respectively
                    JukeBot.isSelfHosted = true;
                    commands.remove("patreon");
                } else {
                    Helpers.monitorThread.scheduleAtFixedRate(Helpers::monitorPledges, 0, 1, TimeUnit.DAYS);
                }

                if (app.getIdLong() == 314145804807962634L || JukeBot.isSelfHosted)
                    JukeBot.playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
            });
        }
    }

}
