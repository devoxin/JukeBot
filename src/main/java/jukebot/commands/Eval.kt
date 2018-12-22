package jukebot.commands

import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory

@CommandProperties(description = "Evaluate arbitrary code.", developerOnly = true)
class Eval : Command {

    private val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine
    private val db = Database()

    override fun execute(context: Context) {
        val bindings = mapOf(
                "ctx" to context,
                "jda" to context.jda,
                "sm" to JukeBot.shardManager,
                "db" to db
        )

        val bindString = bindings.map { "val ${it.key} = bindings[\"${it.key}\"] as ${it.value.javaClass.kotlin.qualifiedName}" }.joinToString("\n")
        val bind = engine.createBindings()
        bind.putAll(bindings)

        try {
            val result = engine.eval("$bindString\n${context.argString}", bind)
            context.channel.sendMessage("```\n$result```").queue(null) {
                context.embed("Response Error", it.toString())
            }
        } catch (e: Exception) {
            context.channel.sendMessage("Engine Error\n```\n$e```").queue(null) {
                context.embed("Response Error", it.toString())
            }
        }
    }

}