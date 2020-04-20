package jukebot.commands.misc

import jukebot.JukeBot
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory
import java.util.concurrent.CompletableFuture

@CommandProperties(description = "Evaluate arbitrary code.", developerOnly = true)
class Eval : Command(ExecutionType.STANDARD) {

    private val engine = KotlinJsr223JvmLocalScriptEngineFactory().scriptEngine

    override fun execute(context: Context) {
        val bindings = mapOf(
            "ctx" to context,
            "jda" to context.jda,
            "sm" to JukeBot.shardManager
        )

        val bindString = bindings.map { "val ${it.key} = bindings[\"${it.key}\"] as ${it.value.javaClass.kotlin.qualifiedName}" }.joinToString("\n")
        val bind = engine.createBindings()
        bind.putAll(bindings)

        try {
            val result = engine.eval("$bindString\n${context.originalArgs}", bind)
                ?: return context.message.addReaction("👌").queue()

            if (result is CompletableFuture<*>) {
                context.channel.sendMessage("```\nCompletableFuture<Pending>```").queue { m ->
                    result.whenComplete { r, ex ->
                        val post = ex ?: r
                        m.editMessage("```\n$post```").queue()
                    }
                }
            } else {
                context.channel.sendMessage("```\n$result```").queue(null) {
                    context.embed("Response Error", it.toString())
                }
            }
        } catch (e: Exception) {
            context.channel.sendMessage("Engine Error\n```\n$e```").queue(null) {
                context.embed("Response Error", it.toString())
            }
        }
    }

}