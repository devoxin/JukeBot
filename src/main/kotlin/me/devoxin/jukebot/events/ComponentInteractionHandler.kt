package me.devoxin.jukebot.events

import io.sentry.Sentry
import kotlinx.coroutines.launch
import me.devoxin.jukebot.annotations.ComponentId
import me.devoxin.jukebot.annotations.InteractionHandler
import me.devoxin.jukebot.utils.Scopes
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

@Suppress("UNCHECKED_CAST")
class ComponentInteractionHandler : EventListener {
    private val eventHandlers = HashMap<Class<out Event>, MutableSet<Handler>>()

    init {
        val scanner = Reflections("me.devoxin.jukebot.interactions")
        val classes = scanner.getTypesAnnotatedWith(InteractionHandler::class.java)

        for (clazz in classes) {
            val methods = clazz.methods.mapNotNull { it.kotlinFunction }.filter { it.hasAnnotation<ComponentId>() }

            if (methods.isEmpty()) {
                log.warn("${clazz.name} is annotated with InteractionHandler, but no handle methods were found!")
                continue
            }

            val instance = clazz.getDeclaredConstructor().newInstance()

            for (method in methods) {
                val valueParameters = method.valueParameters

                if (valueParameters.size != 1) {
                    log.warn("${method.name} has an unexpected amount of parameters (${valueParameters.size}, expected 1)")
                    continue
                }

                val eventParameter = valueParameters[0]
                val type = eventParameter.type.jvmErasure.javaObjectType.takeIf(Event::class.java::isAssignableFrom)
                    ?: continue


                val handlerSet = eventHandlers.computeIfAbsent(type as Class<Event>) { mutableSetOf() }
                val componentId = method.findAnnotation<ComponentId>()!!.value
                handlerSet.add(Handler(componentId, method, instance))

                log.info("Registered handler for componentId $componentId")
            }
        }
    }

    override fun onEvent(event: GenericEvent) {
        if (event !is GenericComponentInteractionCreateEvent) {
            return
        }

        for ((evt, handlers) in eventHandlers) {
            if (!evt.isAssignableFrom(event::class.java)) {
                continue
            }

            for (handler in handlers) {
                if (handler.componentId == event.componentId) {
                    handler(event)
                }
            }
        }
    }

    private inner class Handler(
        val componentId: String,
        private val method: KFunction<*>,
        private val thisArg: Any
    ) {
        operator fun invoke(vararg args: Any) {
            if (method.isSuspend) {
                Scopes.IO.launch {
                    method.call(thisArg, *args)
                }.invokeOnCompletion { err ->
                    if (err != null) {
                        Sentry.capture(err)
                        err.printStackTrace()
                    }
                }
            } else {
                method.call(thisArg, *args)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ComponentInteractionHandler::class.java)
    }
}
