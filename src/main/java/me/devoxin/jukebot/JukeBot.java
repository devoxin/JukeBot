/*
   Copyright 2023 devoxin

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

package me.devoxin.jukebot;

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
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import io.sentry.Sentry;
import io.sentry.SentryClient;
import me.devoxin.jukebot.audio.AudioHandler;
import me.devoxin.jukebot.audio.sourcemanagers.caching.CachingSourceManager;
import me.devoxin.jukebot.audio.sourcemanagers.deezer.DeezerAudioSourceManager;
import me.devoxin.jukebot.audio.sourcemanagers.mixcloud.MixcloudAudioSourceManager;
import me.devoxin.jukebot.audio.sourcemanagers.pornhub.PornHubAudioSourceManager;
import me.devoxin.jukebot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager;
import me.devoxin.jukebot.framework.Command;
import me.devoxin.jukebot.framework.Option;
import me.devoxin.jukebot.handlers.ActionWaiter;
import me.devoxin.jukebot.handlers.CommandHandler;
import me.devoxin.jukebot.handlers.EventHandler;
import me.devoxin.jukebot.patreon.PatreonAPI;
import me.devoxin.jukebot.utils.*;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JukeBot {
    private static final Logger logger = LoggerFactory.getLogger("JukeBot");

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

    public static void main(final String[] args) {
        Thread.currentThread().setName("JukeBot");
        printBanner();

        final String jarLocation = JukeBot.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        System.setProperty("kotlin.script.classpath", jarLocation);

        Message.suppressContentIntentWarning();
        RestAction.setPassContext(false);
        RestAction.setDefaultFailure((e) -> {
        });

        final String token = config.getToken();
        selfId = parseUserIdFromToken(token);
        isSelfHosted = selfId != 249303797371895820L && selfId != 314145804807962634L;
        final EnumSet<GatewayIntent> enabledIntents = IntentHelper.INSTANCE.getEnabledIntents();

        // TODO: Change this.
        final String activityStatus = selfId != 249303797371895820L
            ? config.getDefaultPrefix() + "help | " + Constants.WEBSITE
            : "@JukeBot help | " + Constants.WEBSITE;

        final DefaultShardManagerBuilder shardManagerBuilder = DefaultShardManagerBuilder.create(token, enabledIntents)
                .setShardsTotal(-1)
                .addEventListeners(new CommandHandler(), new EventHandler(), waiter)
                .setMemberCachePolicy(MemberCachePolicy.VOICE)
                .disableCache(
                    CacheFlag.ACTIVITY,
                    CacheFlag.CLIENT_STATUS,
                    CacheFlag.EMOJI,
                    CacheFlag.FORUM_TAGS,
                    CacheFlag.ROLE_TAGS,
                    CacheFlag.ONLINE_STATUS,
                    CacheFlag.SCHEDULED_EVENTS,
                    CacheFlag.STICKER
                )
                .setActivityProvider((i) -> Activity.listening(activityStatus))
                .setBulkDeleteSplittingEnabled(false);

        final String os = System.getProperty("os.name").toLowerCase();
        final String arch = System.getProperty("os.arch");

        if ((os.contains("windows") || os.contains("linux")) && !arch.equalsIgnoreCase("arm") && !arch.equalsIgnoreCase("arm-linux")) {
            logger.info("System supports NAS, enabling...");
            shardManagerBuilder.setAudioSendFactory(new NativeAudioSendFactory());
        }

        shardManager = shardManagerBuilder.build();
        setupSelf();
        setupAudioSystem();
        loadApis();

        final List<CommandData> commands = CommandHandler.Companion.getCommands().values().stream()
            .filter(cmd -> cmd.getProperties().slashCompatible())
            .map(cmd -> {
                final SlashCommandData data = Commands.slash(cmd.getName().toLowerCase(), cmd.getProperties().description());

                for (final Option option : cmd.getOptions()) {
                    data.addOption(option.type(), option.name(), option.description(), option.required());
                }

                return data;
            })
            .collect(Collectors.toList());

        shardManager.getShardById(0).updateCommands().addCommands(commands).queue(
            unused -> logger.info("Synced {} commands with Discord.", commands.size()),
            ex -> logger.error("Failed to sync commands with Discord.", ex)
        );
    }

    private static void printBanner() {
        final String os = System.getProperty("os.name");
        final String arch = System.getProperty("os.arch");
        final String banner = Helpers.INSTANCE.readFile("banner.txt", "");
        final String version = Helpers.INSTANCE.getVersion();

        System.out.printf("%s\nRevision %s | JDA %s | Lavaplayer %s | SQLite %s | %s-bit JVM | %s %s\n\n",
                banner, version, JDAInfo.VERSION, PlayerLibrary.VERSION, SQLiteJDBCLoader.getVersion(), System.getProperty("sun.arch.data.model"), os, arch);
    }

    private static void loadApis() {
        if (config.contains("patreon") && !isSelfHosted) {
            logger.debug("Config has patreon key, loading patreon API...");
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

        if (config.contains("deezer_key")) {
            playerManager.registerSourceManager(new DeezerAudioSourceManager(config.get("deezer_key", null)));
        }

        playerManager.registerSourceManager(new MixcloudAudioSourceManager());

        if (config.getNsfwEnabled()) {
            playerManager.registerSourceManager(new PornHubAudioSourceManager());
        }

        if (config.contains("spotify_client") && config.contains("spotify_secret")) {
            final String client = config.get("spotify_client", null);
            final String secret = config.get("spotify_secret", null);
            playerManager.registerSourceManager(new SpotifyAudioSourceManager(client, secret));
        }

        if (config.getYoutubeEnabled()) {
            final YoutubeAudioSourceManager youtubeAudioSourceManager = new YoutubeAudioSourceManager();

            if (config.getIpv6Block() != null && !config.getIpv6Block().isEmpty()) {
                logger.info("Using IPv6 block with RotatingNanoIpRoutePlanner!");
                final List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(config.getIpv6Block()));
                final RotatingNanoIpRoutePlanner planner = new RotatingNanoIpRoutePlanner(blocks);
                new YoutubeIpRotatorSetup(planner).forSource(youtubeAudioSourceManager).setup();
            }

            playerManager.registerSourceManager(youtubeAudioSourceManager);
        }

        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new GetyarnAudioSourceManager());

        final HttpAudioSourceManager httpAudioSourceManager = new HttpAudioSourceManager();
        httpAudioSourceManager.configureBuilder(cfg -> {
            final String proxyHost = config.opt("proxy_host", null);
            final int proxyPort = config.getInt("proxy_port", 3128);
            final String proxyAuthUser = config.opt("proxy_user", null);
            final String proxyAuthPassword = config.get("proxy_password", "");

            if (proxyHost != null && !proxyHost.isEmpty()) {
                cfg.setProxy(new HttpHost(proxyHost, proxyPort));

                if (proxyAuthUser != null && !proxyAuthUser.isEmpty()) {
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials(proxyAuthUser, proxyAuthPassword));
                    cfg.setDefaultCredentialsProvider(credentialsProvider);
                }
            }
        });
        playerManager.registerSourceManager(httpAudioSourceManager);
    }

    private static void setupSelf() {
        final ApplicationInfo appInfo = shardManager.retrieveApplicationInfo().complete();
        botOwnerId = appInfo.getOwner().getIdLong();

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

    private static long parseUserIdFromToken(final String token) {
        final byte[] encoded = Base64.getDecoder().decode(token.split("\\.")[0]);
        final String idString = new String(encoded);
        return Long.parseLong(idString);
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

    public static String getSearchProvider() {
        return config.getYoutubeEnabled() ? "ytsearch" : "dzsearch";
    }

    // TODO: this needs to be moved to player manager perhaps
    public static AudioItem searchAlternate(final String query) {
        try {
            if (config.getYoutubeEnabled()) {
                return playerManager.source(YoutubeAudioSourceManager.class).loadItem(playerManager, new AudioReference("ytsearch:" + query, null));
            }

            return playerManager.source(DeezerAudioSourceManager.class).getSearch(query);
        } catch (final IOException ioe) {
            return null;
        }
    }
}
