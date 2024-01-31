package me.devoxin.jukebot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import io.sentry.Sentry
import me.devoxin.flight.api.CommandClient
import me.devoxin.jukebot.events.EventHandler
import me.devoxin.jukebot.events.FlightEventAdapter
import me.devoxin.jukebot.integrations.flight.CustomPrefixProvider
import me.devoxin.jukebot.integrations.patreon.PatreonAPI
import me.devoxin.jukebot.models.Config
import me.devoxin.jukebot.utils.Helpers.readFile
import me.devoxin.jukebot.utils.Helpers.version
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.GatewayIntent.MESSAGE_CONTENT
import net.dv8tion.jda.api.requests.RestAction
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteJDBCLoader
import kotlin.system.exitProcess

object Launcher {
    private val log = LoggerFactory.getLogger(Launcher::class.java)

    lateinit var config: Config
    val launchTime = System.currentTimeMillis()

    lateinit var commandClient: CommandClient
    lateinit var shardManager: ExtendedShardManager
    lateinit var playerManager: ExtendedAudioPlayerManager

    var patreonApi: PatreonAPI? = null

    val isSelfHosted: Boolean by lazy {
        shardManager.botId != 249303797371895820L && shardManager.botId != 314145804807962634L
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val jarPath = javaClass.protectionDomain.codeSource.location.path
        val jarName = jarPath.substring(jarPath.lastIndexOf("/") + 1)

        val options = Options().apply {
            addOption("a", "disable-nas", false, "Disables the native audio send system.")
            addOption("b", "banner", false, "Displays the banner and immediately exits.")
            addOption("c", "config", true, "Specify the path of the config.properties file.")
            addOption("d", "disable-youtube-delegate", false, "Disables delegating Spotify tracks to YouTube.")
            addOption("h", "help", false, "Displays command line arguments.")
            addOption("m", "enable-message-content", false, "Enables the 'MESSAGE_CONTENT' intent.")
            addOption("n", "enable-nsfw", false, "Enables NSFW audio sources.")
            addOption("s", "sync-commands", false, "Sync commands with Discord on launch.")
            addOption("u", "disable-http", false, "Disables the HTTP source manager.")
            addOption("y", "disable-youtube", false, "Disables the YouTube source manager.")
        }

        val parser = DefaultParser()
        val helpFormatter = HelpFormatter()
        val parsed = runCatching {
            parser.parse(options, args)
        }.getOrElse {
            if (it is ParseException) {
                helpFormatter.printHelp("java -jar $jarName", options)
                exitProcess(1)
            }

            throw it
        }

        if (parsed.hasOption("help")) {
            helpFormatter.printHelp("java -jar $jarName", options)
            exitProcess(0)
        }

        printBanner()

        if (parsed.hasOption("banner")) {
            exitProcess(0)
        }

        config = Config.load(parsed.getOptionValue("config") ?: "config.properties")

        Message.suppressContentIntentWarning()
        RestAction.setPassContext(false)
        RestAction.setDefaultFailure { }

        commandClient = CommandClient.builder()
            .setPrefixProvider(CustomPrefixProvider())
            .setPrefixes(config.defaultPrefix)
            .addEventListeners(FlightEventAdapter())
            .configureDefaultHelpCommand { enabled = true }
            .registerDefaultParsers()
            .build()

        shardManager = ExtendedShardManager.create(config.token) {
            setActivityProvider { Activity.listening("/help") }
            addEventListeners(commandClient, EventHandler())

            if (!parsed.hasOption("disable-nas")) {
                log.info("enabling native audio send system...")
                setAudioSendFactory(NativeAudioSendFactory())
            }

            if (parsed.hasOption("enable-message-content")) {
                log.info("enabling message content...")
                enableIntents(MESSAGE_CONTENT)
            }
        }

        playerManager = ExtendedAudioPlayerManager(
            disableYoutube = parsed.hasOption("disable-youtube"),
            disableYoutubeDelegate = parsed.hasOption("disable-youtube-delegate"),
            disableHttp = parsed.hasOption("disable-http"),
            enableNsfw = parsed.hasOption("enable-nsfw")
        )

        if ("patreon" in config && !isSelfHosted) {
            log.info("initialising patreon integration...")
            patreonApi = PatreonAPI(config["patreon"])
        }

        if (config.sentryDsn?.isNotEmpty() == true) {
            Sentry.init(config.sentryDsn).apply {
                release = version
            }
        }

        if (isSelfHosted) {
            commandClient.commands.remove("patreon")
            commandClient.commands.remove("verify")
            commandClient.commands.remove("feedback")
        }

        if (parsed.hasOption("sync-commands")) {
            log.info("syncing commands...")
            val slashCommands = commandClient.commands.toDiscordCommands()
            shardManager.shards[0].updateCommands().addCommands(slashCommands).queue(
                { log.info("synced ${slashCommands.size} commands with discord") },
                { log.error("failed to sync commands with discord", it) }
            )
        }

        commandClient.commands.register("me.devoxin.jukebot.commands")
    }

    private fun printBanner() {
        val os = System.getProperty("os.name")
        val arch = System.getProperty("os.arch")
        val banner = readFile("banner.txt", "")

        System.out.printf(
            "%s\nRevision %s | %s-bit JVM | %s %s\nJDA %s | Lavaplayer %s | SQLite %s\n\n",
            banner,
            version,
            System.getProperty("sun.arch.data.model"),
            os,
            arch,
            JDAInfo.VERSION,
            PlayerLibrary.VERSION,
            SQLiteJDBCLoader.getVersion()
        )
    }
}
