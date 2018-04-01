package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(aliases = {"bb", "bassboostmedaddy", "needsmorebass", "hnnng"}, description = "Bass boosts the audio")
public class BassBoost implements Command {

    @Override
    public void execute(GuildMessageReceivedEvent e, String query) {
        AudioHandler handler = JukeBot.getPlayer(e.getGuild().getAudioManager());


        //System.out.println(Float.parseFloat(query));
        handler.equalizer.setGain(0, 0.50F);
        handler.equalizer.setGain(1, 0.25F);
        //handler.equalizer.setGain(2, 0.10F);

        e.getChannel().sendMessage("bass boosting!").queue();
    }
}
