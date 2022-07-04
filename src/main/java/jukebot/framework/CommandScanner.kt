package jukebot.framework

import com.google.common.reflect.ClassPath
import jukebot.JukeBot
import org.slf4j.LoggerFactory

class CommandScanner(private val pkg: String) {
    fun scan(): Map<String, Command> {
        val classes = ClassPath.from(this::class.java.classLoader).getTopLevelClassesRecursive(pkg)
        logger.debug("Discovered ${classes.size} commands")

        return classes
            .asSequence()
            .map { it.load() }
            .map { it.getDeclaredConstructor().newInstance() as Command }
            .filter { it.properties.enabled && (JukeBot.config.nsfwEnabled || !it.properties.nsfw) }
            .map(::loadSubCommands)
            .associateBy { it.name.lowercase() }
    }

    private fun loadSubCommands(cmd: Command): Command {
        val methods = cmd::class.java.methods.filter { it.isAnnotationPresent(SubCommand::class.java) }
        logger.debug("Discovered ${methods.size} subcommands for command ${cmd.name}")

        for (meth in methods) {
            val annotation = meth.getAnnotation(SubCommand::class.java)
            val trigger = annotation.trigger.lowercase()
            val description = annotation.description

            val wrapper = MethodWrapper(description, meth, cmd)
            cmd.subcommands[trigger] = wrapper
        }

        return cmd
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandScanner::class.java)
    }
}
