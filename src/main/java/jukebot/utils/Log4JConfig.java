package jukebot.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.net.URI;

/**
 * Configuration class for Log4J2 (implementation of SLF4J)
 * @author ThatsNoMoon
 */
@Plugin(name = "CustomConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class Log4JConfig extends ConfigurationFactory {

    /* Level of jukebot's log messages (everything through the 'JukeBot' logger) to be displayed */
    private final Level JUKEBOT_LOG_LEVEL;
    /* Level of library log messages (everything except through the 'JukeBot' logger--JDA and Lavaplayer) to be displayed */
    private final Level LIB_LOG_LEVEL;

    /**
     * Log config constructor for individual control of JukeBot's log level and 3rd party library log level
     * @param jukebotLogLevel Minimum level of JukeBot's log messages to log
     * @param libLogLevel Minimum level of library (JDA and Lavaplayer) log messages to log
     * @author ThatsNoMoon
     */
    public Log4JConfig(Level jukebotLogLevel, Level libLogLevel) {
        JUKEBOT_LOG_LEVEL = jukebotLogLevel;
        LIB_LOG_LEVEL = libLogLevel;
    }

    /**
     * Log config constructor for optional log level configuration
     * @param minLogLevel Minimum level of log messages to log
     * @author ThatsNoMoon
     */
    public Log4JConfig(Level minLogLevel) {
        JUKEBOT_LOG_LEVEL = minLogLevel;
        LIB_LOG_LEVEL = minLogLevel;
    }

    /**
     * Log config constructor with default log level of INFO
     * @author ThatsNoMoon
     */
    public Log4JConfig() {
        JUKEBOT_LOG_LEVEL = Level.DEBUG;
        LIB_LOG_LEVEL = Level.INFO;
    }


    private Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {

        builder.setConfigurationName(name);
        /* Only internal Log4J2 messages with level ERROR will be logged */
        builder.setStatusLevel(Level.ERROR);
        /* Create appender that logs to System.out */
        AppenderComponentBuilder appenderBuilder = builder.newAppender("STDOUT", "CONSOLE")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        /* Create pattern for log messages */
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "[%d{HH:mm:ss}] (%c{1}) [%level] %msg%n%throwable"));
                                         /*timestamp  logger name  level   log message & optional throwable */
        builder.add(appenderBuilder);
        /* Create logger that uses STDOUT appender */
        builder.add(builder.newLogger("JukeBot", JUKEBOT_LOG_LEVEL)
                .add(builder.newAppenderRef("STDOUT"))
                .addAttribute("additivity", false));
        /* Create root logger--messages not from the above logger will all go through this one */
        builder.add(builder.newRootLogger(LIB_LOG_LEVEL).add(builder.newAppenderRef("STDOUT")));
        return builder.build();
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
        return createConfiguration(name, builder);
    }

    @Override
    protected String[] getSupportedTypes() {
        return new String[] {"*"};
    }
}