/*
   Copyright 2018 Kromatic

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package jukebot;

import com.patreon.PatreonAPI;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.audioutilities.*;
import jukebot.utils.Helpers;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.awt.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class JukeBot {

    /* Bot-Related*/
    public static final String VERSION = "6.3.0";
    public static final Long startTime = System.currentTimeMillis();
    public static boolean isReady = false;
    public static Logger LOG = LoggerFactory.getLogger("JukeBot");
    public static Properties config;

    public static Color embedColour;
    public static Long botOwnerId = 0L;
    public static boolean isSelfHosted = false;

    /* Operation-Related */
    public static PatreonAPI patreonApi;
    public static SpotifyAPI spotifyApi;
    public static YouTubeAPI youTubeApi;
    private static final ConcurrentHashMap<Long, AudioHandler> players = new ConcurrentHashMap<>();
    public static final ActionWaiter waiter = new ActionWaiter();
    public static AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public static ShardManager shardManager;


    public static void main(final String[] args) throws Exception {
        Thread.currentThread().setName("JukeBot-Main");
        printBanner();

        config = Helpers.readConfig();

        final YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager();
        yt.setPlaylistPageCount(Integer.MAX_VALUE);

        embedColour = Color.decode(config.getProperty("color", "0x1E90FF"));
        spotifyApi = new SpotifyAPI(config.getProperty("spotify_client", ""), config.getProperty("spotify_secret", ""));
        youTubeApi = new YouTubeAPI(config.getProperty("youtube", ""), yt);
        createPatreonApi(config.getProperty("patreon"));

        if (isNSFWEnabled()) {
            playerManager.registerSourceManager(new PornHubAudioSourceManager());
        }

        if (spotifyApi.credentialsProvided()) {
            playerManager.registerSourceManager(new SpotifyAudioSourceManager(spotifyApi, 2));
        }

        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.LOW);
        playerManager.getConfiguration().setOpusEncodingQuality(9);
        playerManager.getConfiguration().setFilterHotSwapEnabled(true);

        playerManager.registerSourceManager(yt);
        AudioSourceManagers.registerRemoteSources(playerManager);

        Database.setupDatabase();

        DefaultShardManagerBuilder shardManagerBuilder = new DefaultShardManagerBuilder()
                .setToken(config.getProperty("token"))
                .setShardsTotal(-1)
                .addEventListeners(new CommandHandler(), waiter)
                .setGame(Game.listening(getDefaultPrefix() + "help | jukebot.xyz"));

        final String os = System.getProperty("os.name").toLowerCase();
        final String arch = System.getProperty("os.arch");

        if ((os.contains("windows") || os.contains("linux")) && !arch.equalsIgnoreCase("arm") && !arch.equalsIgnoreCase("arm-linux")) {
            LOG.info("System supports NAS, enabling...");
            shardManagerBuilder.setAudioSendFactory(new NativeAudioSendFactory());
        }

        shardManager = shardManagerBuilder.build();
    }


    private static void printBanner() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        String banner = Helpers.readFile("banner.txt");

        if (banner == null) {
            banner = "";
        }

        LOG.info("\n" + banner + "\n" +
                "JukeBot v" + VERSION +
                " | JDA " + JDAInfo.VERSION +
                " | Lavaplayer " + PlayerLibrary.VERSION +
                " | SQLite " + SQLiteJDBCLoader.getVersion() +
                " | " + System.getProperty("sun.arch.data.model") + "-bit JVM" +
                " | " + os + " " + arch + "\n");
    }

    public static ConcurrentHashMap<Long, AudioHandler> getPlayers() {
        return players;
    }

    public static boolean hasPlayer(final long guildId) {
        return players.containsKey(guildId);
    }

    public static AudioHandler getPlayer(final AudioManager manager) {

        AudioHandler handler = players.computeIfAbsent(manager.getGuild().getIdLong(),
                v -> new AudioHandler(manager.getGuild().getIdLong(), playerManager.createPlayer()));

        if (manager.getSendingHandler() == null)
            manager.setSendingHandler(handler);

        return handler;

    }

    public static void removePlayer(final long guildId) {
        if (JukeBot.hasPlayer(guildId)) {
            players.remove(guildId).cleanup();
        }
    }

    public static void createPatreonApi(String key) {
        patreonApi = new PatreonAPI(key);
    }

    public static String getDefaultPrefix() {
        return config.getProperty("prefix");
    }

    public static Boolean isNSFWEnabled() {
        return config.getProperty("nsfw", "true").equalsIgnoreCase("true");
    }

}
