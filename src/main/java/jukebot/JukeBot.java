/*
   Copyright 2022 devoxin

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
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import io.sentry.Sentry;
import io.sentry.SentryClient;
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
import jukebot.utils.*;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JukeBot {
    public static final Logger log = LoggerFactory.getLogger("JukeBot");

    /* Bot-Related*/
    public static final long startTime = System.currentTimeMillis();
    public static final Config config = Config.Companion.load();

    public static long selfId = 0L;
    public static long botOwnerId = 0L;
    public static boolean isSelfHosted = false;

    /* Operation-Related */
    public static final RequestUtil httpClient = new RequestUtil();
    public static PatreonAPI patreonApi;

    public static final ConcurrentHashMap<Long, AudioHandler> players = new ConcurrentHashMap<>();
    public static final ActionWaiter waiter = new ActionWaiter();
    public static final CustomAudioPlayerManager playerManager = new CustomAudioPlayerManager();
    public static ShardManager shardManager;

    public static void main(final String[] args) throws Exception {
        Thread.currentThread().setName("JukeBot");
        printBanner();

        final String jarLocation = JukeBot.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        System.setProperty("kotlin.script.classpath", jarLocation);

        RestAction.setPassContext(false);
        RestAction.setDefaultFailure((e) -> {
        });

        final String token = config.getToken();
        final EnumSet<GatewayIntent> enabledIntents = IntentHelper.INSTANCE.getEnabledIntents();

        final DefaultShardManagerBuilder shardManagerBuilder = DefaultShardManagerBuilder.create(token, enabledIntents)
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
                .setActivityProvider((i) -> Activity.listening(config.getDefaultPrefix() + "help | " + Constants.WEBSITE))
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
        final String os = System.getProperty("os.name");
        final String arch = System.getProperty("os.arch");
        final String banner = Helpers.INSTANCE.readFile("banner.txt", "");
        final String version = Helpers.INSTANCE.getVersion();

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
        if (config.contains("patreon") && !isSelfHosted) {
            log.debug("Config has patreon key, loading patreon API...");
            patreonApi = new PatreonAPI(config.get("patreon", null));
            // Default should never be used here, but Java insists on the 2nd parameter.
        }

        if (config.getSentryDsn() != null && !config.getSentryDsn().isEmpty()) {
            final SentryClient sentry = Sentry.init(config.getSentryDsn());
            sentry.setRelease(Helpers.INSTANCE.getVersion());
        }
    }

    /**
     * Audio System
     */
    private static void setupAudioSystem() {
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        playerManager.setPlayerCleanupThreshold(30000);
        playerManager.getConfiguration().setFilterHotSwapEnabled(true);

        playerManager.registerSourceManager(new CachingSourceManager());
        playerManager.registerSourceManager(new MixcloudAudioSourceManager());

        if (config.getNsfwEnabled()) {
            playerManager.registerSourceManager(new PornHubAudioSourceManager());
        }

        if (config.contains("spotify_client") && config.contains("spotify_secret")) {
            final String client = config.get("spotify_client", null);
            final String secret = config.get("spotify_secret", null);
            playerManager.registerSourceManager(new SpotifyAudioSourceManager(client, secret));
        }

        AudioSourceManagers.registerRemoteSources(playerManager);

        final YoutubeAudioSourceManager sourceManager = playerManager.source(YoutubeAudioSourceManager.class);
        sourceManager.setPlaylistPageCount(Integer.MAX_VALUE);

        if (config.getIpv6Block() != null && !config.getIpv6Block().isEmpty()) {
            log.info("Using IPv6 block with RotatingNanoIpRoutePlanner!");
            final List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(config.getIpv6Block()));
            final RotatingNanoIpRoutePlanner planner = new RotatingNanoIpRoutePlanner(blocks);
            new YoutubeIpRotatorSetup(planner).forSource(sourceManager).setup();
        }
    }

    private static void setupSelf() {
        final ApplicationInfo appInfo = shardManager.retrieveApplicationInfo().complete();
        selfId = appInfo.getIdLong();
        botOwnerId = appInfo.getOwner().getIdLong();
        isSelfHosted = selfId != 249303797371895820L
                && selfId != 314145804807962634L;

        if (isSelfHosted || selfId == 314145804807962634L) {
            playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        }

        if (isSelfHosted) {
            final Map<String, Command> commandRegistry = CommandHandler.Companion.getCommands();
            commandRegistry.remove("patreon");
            commandRegistry.remove("verify");
            final Command feedback = commandRegistry.remove("feedback");

            if (feedback != null) {
                feedback.destroy();
            }
        }
    }

    public static boolean hasPlayer(final long guildId) {
        return players.containsKey(guildId);
    }

    public static AudioHandler getPlayer(final long guildId) {
        final Guild g = shardManager.getGuildById(guildId);
        Objects.requireNonNull(g, "Guild does not exist for the provided guildId!");

        final AudioHandler handler = players.computeIfAbsent(guildId,
                __ -> new AudioHandler(guildId, playerManager.createPlayer()));

        final AudioManager audioManager = g.getAudioManager();

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
