package me.devoxin.jukebot.commands.misc

import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
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

        val bindString =
            bindings.map { "val ${it.key} = bindings[\"${it.key}\"] as ${it.value.javaClass.kotlin.qualifiedName}" }
                .joinToString("\n")
        val bind = engine.createBindings()
        bind.putAll(bindings)

        try {
            val result = engine.eval("$bindString\n${context.args.gatherNext("code")}", bind)
                ?: return context.react("👌")

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
