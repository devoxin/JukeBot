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

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import jukebot.apis.KSoftAPI;
import jukebot.apis.PatreonAPI;
import jukebot.apis.SpotifyAPI;
import jukebot.apis.YouTubeAPI;
import jukebot.audio.AudioHandler;
import jukebot.audio.sourcemanagers.pornhub.PornHubAudioSourceManager;
import jukebot.utils.Config;
import jukebot.utils.Helpers;
import jukebot.utils.RequestUtil;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class JukeBot {

    /* Bot-Related*/
    public static final String VERSION = "6.4.5";

    public static final Long startTime = System.currentTimeMillis();
    public static boolean isReady = false;
    public static Logger LOG = LoggerFactory.getLogger("JukeBot");
    public static Config config = new Config("config.properties");

    public static Long botOwnerId = 0L;
    public static boolean isSelfHosted = false;

    /* Operation-Related */
    public static final RequestUtil httpClient = new RequestUtil();
    public static PatreonAPI patreonApi;
    public static SpotifyAPI spotifyApi;
    public static YouTubeAPI youTubeApi;
    public static KSoftAPI kSoftAPI;
    //public static LastFM lastFM;

    public static final ConcurrentHashMap<Long, AudioHandler> players = new ConcurrentHashMap<>();
    public static final ActionWaiter waiter = new ActionWaiter();
    public static AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    public static ShardManager shardManager;


    public static void main(final String[] args) throws Exception {
        Thread.currentThread().setName("JukeBot-Main");
        printBanner();

        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setFilterHotSwapEnabled(true);

        Database.setupDatabase();
        registerSourceManagers();
        loadApis();

        DefaultShardManagerBuilder shardManagerBuilder = new DefaultShardManagerBuilder()
                .setToken(config.getToken())
                .setShardsTotal(-1)
                .addEventListeners(new CommandHandler(), waiter)
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.EMOTE, CacheFlag.GAME))
                .setGame(Game.listening(config.getDefaultPrefix() + "help | https://jukebot.serux.pro"));

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
        String banner = Helpers.Companion.readFile("banner.txt", "");

        LOG.info("\n" + banner + "\n" +
                "JukeBot v" + VERSION +
                " | JDA " + JDAInfo.VERSION +
                " | Lavaplayer " + PlayerLibrary.VERSION +
                " | SQLite " + SQLiteJDBCLoader.getVersion() +
                " | " + System.getProperty("sun.arch.data.model") + "-bit JVM" +
                " | " + os + " " + arch + "\n");
    }

    private static void loadApis() {
        if (config.hasKey("patreon")) {
            LOG.debug("Config has patreon key, loading patreon API...");
            createPatreonApi(config.getString("patreon"));
        }

        if (config.hasKey("ksoft")) {
            LOG.debug("Config has ksoft key, loading ksoft API...");
            String key = Objects.requireNonNull(config.getString("ksoft"));
            kSoftAPI = new KSoftAPI(key);
        }

        if (config.hasKey("spotify_client") && config.hasKey("spotify_secret")) {
            LOG.debug("Config has spotify keys, loading spotify API...");
            String client = Objects.requireNonNull(config.getString("spotify_client"));
            String secret = Objects.requireNonNull(config.getString("spotify_secret"));
            spotifyApi = new SpotifyAPI(client, secret);
        }

        if (config.hasKey("youtube")) {
            LOG.debug("Config has youtube key, loading youtube API...");
            String key = Objects.requireNonNull(config.getString("youtube"));
            YoutubeAudioSourceManager sm = playerManager.source(YoutubeAudioSourceManager.class);
            youTubeApi = new YouTubeAPI(key, sm);
        }
    }

    private static void registerSourceManagers() {
        final YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager();
        yt.setPlaylistPageCount(Integer.MAX_VALUE);

        if (config.getNsfwEnabled()) {
            playerManager.registerSourceManager(new PornHubAudioSourceManager());
        }

        playerManager.registerSourceManager(yt);
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager());
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


}
