/*
   Copyright 2020 devoxin

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
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import io.sentry.Sentry;
import jukebot.apis.ksoft.KSoftAPI;
import jukebot.apis.patreon.PatreonAPI;
import jukebot.audio.AudioHandler;
import jukebot.audio.sourcemanagers.caching.CachingSourceManager;
import jukebot.audio.sourcemanagers.mixcloud.MixcloudAudioSourceManager;
import jukebot.audio.sourcemanagers.pornhub.PornHubAudioSourceManager;
import jukebot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager;
import jukebot.framework.Command;
import jukebot.listeners.ActionWaiter;
import jukebot.listeners.CommandHandler;
import jukebot.listeners.EventHandler;
import jukebot.utils.Config;
import jukebot.utils.Helpers;
import jukebot.utils.IntentHelper;
import jukebot.utils.RequestUtil;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class JukeBot {
    /* Bot-Related*/
    public static final long startTime = System.currentTimeMillis();
    public static Logger log = LoggerFactory.getLogger("JukeBot");
    public static Config config = Config.Companion.load();

    public static long selfId = 0L;
    public static long botOwnerId = 0L;
    public static boolean isSelfHosted = false;

    /* Operation-Related */
    public static final RequestUtil httpClient = new RequestUtil();
    public static PatreonAPI patreonApi;
    public static KSoftAPI kSoftAPI;

    public static final ConcurrentHashMap<Long, AudioHandler> players = new ConcurrentHashMap<>();
    public static final ActionWaiter waiter = new ActionWaiter();
    public static CustomAudioPlayerManager playerManager = new CustomAudioPlayerManager();
    public static ShardManager shardManager;

    public static void main(final String[] args) throws Exception {
        Thread.currentThread().setName("JukeBot");
        printBanner();

        String jarLocation = JukeBot.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        //String decodedLocation = URLDecoder.decode(jarLocation, Charset.defaultCharset());
        System.setProperty("kotlin.script.classpath", jarLocation);

        RestAction.setPassContext(false);
        RestAction.setDefaultFailure((e) -> {
        });

        String token = config.getToken();
        EnumSet<GatewayIntent> enabledIntents = IntentHelper.INSTANCE.getEnabledIntents();

        DefaultShardManagerBuilder shardManagerBuilder = DefaultShardManagerBuilder.create(token, enabledIntents)
                .setShardsTotal(-1)
                .addEventListeners(new CommandHandler(), new EventHandler(), waiter)
                .setMemberCachePolicy(MemberCachePolicy.VOICE)
                .disableCache(
                        CacheFlag.ACTIVITY,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.EMOTE,
                        CacheFlag.ROLE_TAGS,
                        CacheFlag.ONLINE_STATUS
                )
                .setActivityProvider((i) -> Activity.listening(config.getDefaultPrefix() + "help | https://jukebot.serux.pro"))
                .setBulkDeleteSplittingEnabled(false);

        final String os = System.getProperty("os.name").toLowerCase();
        final String arch = System.getProperty("os.arch");

        if ((os.contains("windows") || os.contains("linux")) && !arch.equalsIgnoreCase("arm") && !arch.equalsIgnoreCase("arm-linux")) {
            log.info("System supports NAS, enabling...");
            shardManagerBuilder.setAudioSendFactory(new NativeAudioSendFactory());
        }

        shardManager = shardManagerBuilder.build();
        setupSelf();
        setupAudioSystem();
        loadApis();
    }

    private static void printBanner() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        String banner = Helpers.INSTANCE.readFile("banner.txt", "");
        String version = Helpers.INSTANCE.getVersion();

        System.out.printf(
                "%s\nRevision %s | JDA %s | Lavaplayer %s | SQLite %s | %s-bit JVM | %s %s\n\n",
                banner,
                version,
                JDAInfo.VERSION,
                PlayerLibrary.VERSION,
                SQLiteJDBCLoader.getVersion(),
                System.getProperty("sun.arch.data.model"),
                os,
                arch
        );
    }

    private static void loadApis() {
        if (config.contains("patreon")) {
            log.debug("Config has patreon key, loading patreon API...");
            patreonApi = new PatreonAPI(config.get("patreon", null));
            // Default should never be used here, but Java insists on the 2nd parameter.
        }

        if (config.contains("ksoft")) {
            log.debug("Config has ksoft key, loading ksoft API...");
            String key = config.get("ksoft", null);
            kSoftAPI = new KSoftAPI(key);
        }

        if (config.getSentryDsn() != null && !config.getSentryDsn().isEmpty()) {
            Sentry.init(config.getSentryDsn());
            Sentry.getStoredClient().setRelease(Helpers.INSTANCE.getVersion());
        }
    }

    /**
     * Audio System
     */
    private static void setupAudioSystem() {
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setFilterHotSwapEnabled(true);

        registerSourceManagers();

        YoutubeAudioSourceManager sourceManager = playerManager.source(YoutubeAudioSourceManager.class);
        sourceManager.setPlaylistPageCount(Integer.MAX_VALUE);

        if (config.getIpv6Block() != null && !config.getIpv6Block().isEmpty()) {
            log.info("Using IPv6 block with RotatingNanoIpRoutePlanner!");
            List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(config.getIpv6Block()));
            RotatingNanoIpRoutePlanner planner = new RotatingNanoIpRoutePlanner(blocks);
            new YoutubeIpRotatorSetup(planner).forSource(sourceManager).setup();
        }
    }

    private static void registerSourceManagers() {
        playerManager.registerSourceManager(new CachingSourceManager());
        playerManager.registerSourceManager(new MixcloudAudioSourceManager());

        if (config.getNsfwEnabled()) {
            playerManager.registerSourceManager(new PornHubAudioSourceManager());
        }

        YoutubeAudioSourceManager ytasm = new YoutubeAudioSourceManager();

        if (config.contains("spotify_client") && config.contains("spotify_secret")) {
            String client = config.get("spotify_client", null);
            String secret = config.get("spotify_secret", null);
            playerManager.registerSourceManager(new SpotifyAudioSourceManager(client, secret, ytasm));
        }

        playerManager.registerSourceManager(ytasm);
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new GetyarnAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager());
    }

    private static void setupSelf() {
        ApplicationInfo appInfo = shardManager.retrieveApplicationInfo().complete();
        selfId = appInfo.getIdLong();
        botOwnerId = appInfo.getOwner().getIdLong();
        isSelfHosted = appInfo.getIdLong() != 249303797371895820L
                && appInfo.getIdLong() != 314145804807962634L;

        if (isSelfHosted || selfId == 314145804807962634L) {
            playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        }

        if (isSelfHosted) {
            Map<String, Command> commandRegistry = CommandHandler.Companion.getCommands();
            commandRegistry.remove("patreon");
            commandRegistry.remove("verify");
            Command feedback = commandRegistry.remove("feedback");

            if (feedback != null) {
                feedback.destroy();
            }
        } else {
            Helpers.INSTANCE.getMonitor().scheduleAtFixedRate(Helpers.INSTANCE::monitorPledges, 0, 1, TimeUnit.DAYS);
        }
    }

    public static boolean hasPlayer(final long guildId) {
        return players.containsKey(guildId);
    }

    public static AudioHandler getPlayer(final long guildId) {
        Guild g = shardManager.getGuildById(guildId);
        Objects.requireNonNull(g, "getPlayer was given an invalid guildId!");

        AudioHandler handler = players.computeIfAbsent(guildId,
                v -> new AudioHandler(guildId, playerManager.createPlayer()));

        AudioManager audioManager = g.getAudioManager();

        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(handler);
        }

        return handler;
    }

    public static void removePlayer(final long guildId) {
        if (JukeBot.hasPlayer(guildId)) {
            players.remove(guildId).cleanup();
        }
    }
}
